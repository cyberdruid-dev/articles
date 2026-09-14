package com.example.tickets;

import org.jdbscript.IDBSchema;
import org.jdbscript.IDBSchema.IDBRecord;
import org.jdbscript.RecordTools;

public interface TicketsSchema extends IDBSchema {

    ITicketRecord ticket();

    interface ITicketRecord extends IDBRecord {
        ITicketRecord id(Long id);
        ITicketRecord status(String status);
        ITicketRecord assignee(String assignee);
        ITicketRecord subject(String subject);
        ITicketRecord resolution(String resolution);

        // Auto-assigned only if a test doesn't set .id(...) itself - lets tests that never need
        // to reference the id afterward (e.g. the listing tests) leave it out entirely.
        default void defaults(RecordTools tools) {
            id(tools.nextLongId("ticket", 1));
        }
    }
}
