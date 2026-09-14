package com.example.tickets;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("select t from Ticket t where t.assignee = :assignee and t.status = :status")
    List<Ticket> findAssignedOpenTickets(@Param("assignee") String assignee, @Param("status") TicketStatus status);
}
