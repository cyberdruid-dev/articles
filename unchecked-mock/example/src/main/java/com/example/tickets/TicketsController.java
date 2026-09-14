package com.example.tickets;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class TicketsController implements TicketsApi {

    private final TicketRepository tickets;

    public TicketsController(TicketRepository tickets) {
        this.tickets = tickets;
    }

    @Override
    public TicketDTO close(CloseTicketRequest request) {
        Ticket ticket = tickets.findById(request.ticketId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No ticket " + request.ticketId()));

        if (ticket.getStatus() != TicketStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ticket " + request.ticketId() + " is not open");
        }

        ticket.resolve(request.resolution());
        tickets.save(ticket);

        return TicketDTO.from(ticket);
    }

    @Override
    public List<TicketDTO> openTicketsFor(String assignee) {
        return tickets.findAssignedOpenTickets(assignee, TicketStatus.OPEN).stream()
                .map(TicketDTO::from)
                .toList();
    }
}
