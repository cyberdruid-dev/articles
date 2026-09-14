# Nobody Has Ever Checked If Your Mock Is Right

### A test where nothing is guessed.


```java
@Test
public void openTicketsFor_returns_only_that_assignees_tickets() {
    db.resetDB(db -> {
        db.ticket().status("OPEN").assignee(AGENT_ID).subject("Printer jammed");
        db.ticket().status("OPEN").assignee(OTHER_AGENT_ID).subject("VPN drops every few minutes");
    });

    List<TicketDTO> result = ticketsApi.openTicketsFor(AGENT_ID); // real HTTP GET

    assertThat(result).extracting(TicketDTO::subject).containsExactly("Printer jammed");
}
```
*(full class: [link](https://github.com/cyberdruid-dev/articles/blob/main/unchecked-mock/example/src/test/java/com/example/tickets/TicketsWSTest.java))*

Nothing here is mocked. `ticketsApi` isn't a service bean, and it isn't MockMvc quietly faking a
request in-process — it's a typed HTTP client, and the app on the other end is a real, running
server. `db` isn't a mock either — it's a real database, seeded fresh before the call.

A mock would have been faster here, though it's not free either: a mock has to be kept honest as
the code around it changes. But speed isn't the whole reason people reach for one: the less a
test depends on, the more you can trust what it tells you. By that measure, this test is doing
everything backwards. It depends on a running server, a real socket, a real query, a real
database.

So why write it this way on purpose?


Look at those two lines again. The first seeds two rows in an actual table — no XML fixture, no
hand-written INSERT. When the schema itself changes, the matching interface has to change too,
but that's a compiler error until you fix it, not something you can quietly forget. The second
makes a genuine HTTP call — no manually built request, no guessing at the response shape. Both
are typed — a schema interface for the rows, a client for the API. Get a column or a field name
wrong and it won't build.

Building either of those by hand is tedious, unglamorous work — exactly why so many tests skip
it and reach for a mock instead, or don't get written at all.


The test above doesn't know or care that the filtering happens in a database query. It checks
one thing: whether the endpoint returns the right tickets for this state. Because it's the real
endpoint, whatever actually filters — a query, right now — has to get it right for that to be
true. Mock the repository instead, and that piece never runs at all: the controller just relays
whatever the mock was told to return.

And because the test only ever checks the endpoint's behavior, not how it's implemented,
refactoring is free: move the filtering out of the query tomorrow, put it wherever makes more
sense, and the test doesn't need to change.

There's a bill for all this, though. Booting the application, running its migrations, and
waiting on a database — even an in-memory one — takes time: a couple of seconds for something
this small, more for anything bigger. That's genuinely slower than a test that never leaves the
JVM, and no amount of tooling makes that difference disappear.


Not every feature belongs in a test shaped like this one. Here's a way to tell: try writing the
top-to-bottom version first. If it comes together easily — a short seed, one call, an assertion
on the response — that ease is the answer. The feature was simple enough not to need anything
more.

If it's painful instead — the seed sprawls across a dozen tables, the assertion has to account
for effects that have nothing to do with each other, the whole thing takes far too long to even
get building — that pain is signal, not a nuisance to push through. The next question is where
it's coming from. Sometimes it's self-inflicted: too much coupled together, boundaries drawn in
the wrong place, more moving parts than the feature actually needs — fixed by simplifying the
design, not by writing a more elaborate test around it. Sometimes it's inherent to the feature
itself: genuinely non-trivial logic, with no simpler shape to be found no matter how you
rearrange it. Only that second kind earns isolating. Pull it into its own class — sometimes its
own module, with its own tested contract the rest of the system just trusts — and unit-test it
there, on its own terms.

Everything shown so far has been the first case — which is exactly why it never needed anything
more than the test above.


None of this is an argument against mocks in general. Mock what you don't control: a
third-party service you don't run yourself, maybe one you can't even reach from a laptop, or one
you need to fail on demand — a timeout, a 500 — in a way that would be heavy or unreliable to
cause for real. A mock earns its place there, because there's nothing else to run
instead; the real thing is outside your walls.

What doesn't get mocked is anything inside your own walls — your own database, your own object
graph, your own code calling your own code. Not because it's technically impossible; obviously
it isn't. It's the same thing from the start of this piece: the less a test depends on, the
more you can trust what it tells you. That's true at the boundary you don't control. It stops
being true the moment you're back inside your own walls: there, a mock doesn't prove your code
works. It proves your code works *if* the mock's assumption is correct — and nothing checks
whether it actually is. Mock the same dependency in a different test, with a slightly different
assumption, and nothing checks that the two agree with each other either.


So why write it this way on purpose? Because this test can only pass if the real thing actually
works as a whole. A mocked test can pass even when the part it mocked away — the query, or the
integration itself — is broken.


Full example, including what's not shown here: [github.com/cyberdruid-dev/articles/tree/main/unchecked-mock/example](https://github.com/cyberdruid-dev/articles/tree/main/unchecked-mock/example).
