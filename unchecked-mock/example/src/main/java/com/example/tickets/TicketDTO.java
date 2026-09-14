package com.example.tickets;

public record TicketDTO(Long id, TicketStatus status, String assignee, String subject, String resolution) {

    static TicketDTO from(Ticket ticket) {
        return new TicketDTO(ticket.getId(), ticket.getStatus(), ticket.getAssignee(),
                ticket.getSubject(), ticket.getResolution());
    }
}
