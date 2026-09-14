# tickets-example

Companion code for the "top-to-bottom testing" article: one table, Spring MVC, Flyway, a real
running server, a typed Feign client, and [jdbscript](https://jdbscript.org) for test DB seeding.

Deliberately minimal (one table, no service layer) — the point of the example is that this kind
of everyday feature code doesn't need a unit test; the real, running-server test at
`src/test/java/com/example/tickets/TicketsWSTest.java` is the whole test suite for it.

