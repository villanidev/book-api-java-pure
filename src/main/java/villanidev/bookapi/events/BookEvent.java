package villanidev.bookapi.events;

import villanidev.bookapi.mvc.Book;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record BookEvent(
        UUID eventId,
        String bookId,
        Book book,
        Instant timestamp,
        long version,
        CompletableFuture<Void> completionFuture
) {}
