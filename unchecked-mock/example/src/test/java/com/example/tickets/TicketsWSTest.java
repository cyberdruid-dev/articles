package com.example.tickets;

import feign.Feign;
import feign.FeignException;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.jdbscript.IJDBEngine;
import org.jdbscript.JDBEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(
        webEnvironment = RANDOM_PORT, // boots the real app on a real port
        // spring-cloud-openfeign-core is only on the classpath for its SpringMvcContract class
        // (used below to build a plain Feign client by hand); its own autoconfiguration isn't
        // wanted and isn't otherwise usable here without the rest of Spring Cloud.
        properties = "spring.autoconfigure.exclude=org.springframework.cloud.openfeign.FeignAutoConfiguration"
)
public class TicketsWSTest extends AbstractTestNGSpringContextTests {

    private static final long TICKET_ID = 1L;
    private static final long MISSING_TICKET_ID = 99L;
    private static final String AGENT_ID = "agent-42";
    private static final String OTHER_AGENT_ID = "agent-7";

    @LocalServerPort
    private int serverPort;

    @Autowired
    private DataSource dataSource;

    private TicketsApi ticketsApi; // typed HTTP client - every call below is a real network request
    private IJDBEngine<TicketsSchema> db;

    @BeforeClass
    public void setUp() {
        ticketsApi = newTicketsApi();
        db = newTicketsDb();
    }

    private TicketsApi newTicketsApi() {
        return Feign.builder()
                .contract(new SpringMvcContract()) // reuses TicketsApi's own @PostMapping/@RequestBody
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .target(TicketsApi.class, "http://localhost:%d".formatted(serverPort));
    }

    private IJDBEngine<TicketsSchema> newTicketsDb() {
        return JDBEngine.builder(TicketsSchema.class)
                .dataSource(dataSource)
                .build();
    }

    @Test
    public void closingTicket_marks_it_resolved() {
        db.resetDB(db -> {
            db.ticket().id(TICKET_ID).status("OPEN").assignee(AGENT_ID).subject("Printer on 3rd floor is jammed");
        });

        CloseTicketRequest request = new CloseTicketRequest(TICKET_ID, "Replaced toner cartridge");
        TicketDTO result = ticketsApi.close(request); // real HTTP POST

        assertThat(result.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(result.resolution()).isEqualTo("Replaced toner cartridge");
    }

    @Test
    public void closingTicket_fails_when_already_resolved() {
        db.resetDB(db -> {
            db.ticket().id(TICKET_ID).status("RESOLVED").assignee(AGENT_ID).subject("Printer on 3rd floor is jammed");
        });

        CloseTicketRequest request = new CloseTicketRequest(TICKET_ID, "Second attempt");

        assertThatExceptionOfType(FeignException.class)
                .isThrownBy(() -> ticketsApi.close(request))
                .extracting(FeignException::status)
                .isEqualTo(409);
    }

    @Test
    public void closingTicket_fails_when_ticket_does_not_exist() {
        db.cleanupDB();

        CloseTicketRequest request = new CloseTicketRequest(MISSING_TICKET_ID, "N/A");

        assertThatExceptionOfType(FeignException.class)
                .isThrownBy(() -> ticketsApi.close(request))
                .extracting(FeignException::status)
                .isEqualTo(404);
    }

    @Test
    public void openTicketsFor_returns_only_that_assignees_tickets() {
        db.resetDB(db -> {
            db.ticket().status("OPEN").assignee(AGENT_ID).subject("Printer jammed");
            db.ticket().status("OPEN").assignee(OTHER_AGENT_ID).subject("VPN drops every few minutes");
        });

        List<TicketDTO> result = ticketsApi.openTicketsFor(AGENT_ID);

        assertThat(result).extracting(TicketDTO::subject).containsExactly("Printer jammed");
    }

    @Test
    public void openTicketsFor_excludes_resolved_tickets() {
        db.resetDB(db -> {
            db.ticket().status("OPEN").assignee(AGENT_ID).subject("Printer jammed");
            db.ticket().status("RESOLVED").assignee(AGENT_ID).subject("VPN drops").resolution("Restarted router");
        });

        List<TicketDTO> result = ticketsApi.openTicketsFor(AGENT_ID);

        assertThat(result).extracting(TicketDTO::subject).containsExactly("Printer jammed");
    }

    @Test
    public void openTicketsFor_returns_empty_list_when_none_match() {
        db.cleanupDB();

        List<TicketDTO> result = ticketsApi.openTicketsFor(AGENT_ID);

        assertThat(result).isEmpty();
    }
}
