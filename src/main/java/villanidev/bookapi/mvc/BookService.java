package villanidev.bookapi.mvc;

import villanidev.bookapi.batchworker.BatchWriteWorker;
import villanidev.bookapi.events.BookEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

public class BookService {
    private final BlockingQueue<BookEvent> writeQueue = new LinkedBlockingQueue<>(10_000);
    private final ExecutorService workerExecutor = Executors.newSingleThreadExecutor(
            Thread.ofVirtual()
                    .name("workerVthread-", 0)
                    .factory()
    );
    private final BookRepositoryWithCache repository;

    public BookService(BookRepositoryWithCache repository) {
        this.repository = repository;
        this.workerExecutor.submit(new BatchWriteWorker(writeQueue, repository.getDataSource(), repository.getCache()));
    }

    public CompletableFuture<Void> save(Book book) {
        String id = book.id() == null ? UUID.randomUUID().toString() : book.id();
        Book newBook = new Book(id, book.title(), book.author(), book.year());
        CompletableFuture<Void> future = new CompletableFuture<>();
        boolean newSaveBookEvent = writeQueue.offer(
                new BookEvent(
                        UUID.randomUUID(),
                        newBook.id(),
                        newBook,
                        Instant.now(),
                        System.currentTimeMillis(), // Version as timestamp
                        future
                )
        );

        if (!newSaveBookEvent) {
            future.completeExceptionally(new IllegalStateException("Write queue full"));
        }
        return future;
    }

    public Optional<Book> findById(String id) {
        return repository.findById(id);
    }

    public List<Book> findAll() {
        return repository.findAll();
    }
}
