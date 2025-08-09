package villanidev.bookapi.mvc;

import villanidev.bookapi.batchworker.BatchWriteWorker;
import villanidev.bookapi.events.BookEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class BookService {
    private final BlockingQueue<BookEvent> writeQueue = new LinkedBlockingQueue<>(10_000);
    private final ExecutorService workerExecutor = Executors.newSingleThreadExecutor(
            Thread.ofVirtual()
                    .name("workerVthread-", 0)
                    .factory()
    );
    //private final BookRepositoryWithCache repository;
    private final ChronicleBookRepository repository;

    public BookService(ChronicleBookRepository repository) {
        this.repository = repository;
        //this.workerExecutor.submit(new BatchWriteWorker(writeQueue, repository.getDataSource(), repository.getCache()));
        this.workerExecutor.submit(new BatchWriteWorker(writeQueue, repository));
    }

    /*public CompletableFuture<Void> save(Book book) {
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
    }*/

    public void save(Book book) {
        System.out.println("Saving book: " + Thread.currentThread().getName());
        String id = book.id() == null ? UUID.randomUUID().toString() : book.id();
        Book newBook = new Book(id, book.title(), book.author(), book.year());
        boolean newSaveBookEvent = writeQueue.offer(
                new BookEvent(
                        UUID.randomUUID(),
                        newBook.id(),
                        newBook,
                        Instant.now(),
                        System.currentTimeMillis(), // Version as timestamp
                        null
                )
        );

        if (!newSaveBookEvent) {
            throw new IllegalStateException("Write queue full");
        }
    }

    public Optional<Book> findById(String id) {
        return repository.findById(id);
    }

    public List<Book> findAll() {
        return repository.findAll();
    }
}
