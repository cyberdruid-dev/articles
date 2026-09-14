package com.example.tickets;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Shared contract: implemented by {@link TicketsController} on the server side, and used
 * directly (via Spring's own MVC annotations) to build a typed Feign client on the test side —
 * one interface, not two near-duplicate ones.
 */
public interface TicketsApi {

    @PostMapping(value = "/tickets/close", consumes = "application/json")
    TicketDTO close(@RequestBody CloseTicketRequest request);

    @GetMapping("/tickets")
    List<TicketDTO> openTicketsFor(@RequestParam("assignee") String assignee);
}
