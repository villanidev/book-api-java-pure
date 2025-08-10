package villanidev.bookapi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class BookService {
    public static final int BATCH_WORKERS = 2;
    private final BlockingQueue<Book> writeQueue = new LinkedBlockingQueue<>(10_000);

    private final BookRepositoryWithCache repository;

    public BookService(BookRepositoryWithCache repository) {
        this.repository = repository;
        for (int i = 0; i < BATCH_WORKERS; i++) {
            ExecutorService batchExecutor = Executors.newFixedThreadPool(
                    BATCH_WORKERS,
                    Thread.ofVirtual()
                            .name("batchWorkerVthread-", 0)
                            .factory()
            );
            batchExecutor.submit(new BatchWriteWorker(writeQueue, repository));
        }
    }

    public void save(Book book) {
        System.out.println("Saving book: " + Thread.currentThread().getName());
        String id = book.id() == null ? UUID.randomUUID().toString() : book.id();
        Book newBook = new Book(id, book.title(), book.author(), book.year());
        boolean newSaveBookEvent = writeQueue.offer(newBook);

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
