package com.example.tickets;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ticket")
public class Ticket {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    private String assignee;
    private String subject;
    private String resolution;

    protected Ticket() {
        // JPA
    }

    public Ticket(Long id, TicketStatus status, String assignee, String subject) {
        this.id = id;
        this.status = status;
        this.assignee = assignee;
        this.subject = subject;
    }

    public Long getId() {
        return id;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public String getAssignee() {
        return assignee;
    }

    public String getSubject() {
        return subject;
    }

    public String getResolution() {
        return resolution;
    }

    /** No complexity worth isolating here on purpose - that's the point of the example. */
    public void resolve(String resolution) {
        this.status = TicketStatus.RESOLVED;
        this.resolution = resolution;
    }
}
