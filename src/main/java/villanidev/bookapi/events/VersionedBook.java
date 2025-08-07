package villanidev.bookapi.events;

import villanidev.bookapi.mvc.Book;

import java.time.Instant;

public record VersionedBook(
        Book book,
        long version,
        Instant lastUpdated
) {}
