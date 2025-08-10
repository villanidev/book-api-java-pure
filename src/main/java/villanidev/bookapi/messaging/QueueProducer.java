package villanidev.bookapi.messaging;

import villanidev.bookapi.Book;

import java.util.concurrent.*;

public class QueueProducer {
    private final BlockingQueue<Book> writeQueue;
    private final ExecutorService executorService;

    public QueueProducer(int queueSize) {
        this.writeQueue = new LinkedBlockingQueue<>(queueSize);
        this.executorService = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("producer-", 0).factory()
        );
    }

    public CompletableFuture<Void> enqueue(Book book) {
        System.out.println("Placing into queue: " + Thread.currentThread().getName());
        CompletableFuture<Void> future = new CompletableFuture<>();
        executorService.execute(() -> {
            try {
                writeQueue.put(book); // Blocks if queue full
                future.complete(null);
            } catch (InterruptedException e) {
                future.completeExceptionally(e);
                Thread.currentThread().interrupt();
            }
        });
        return future;
    }

    public Book take() throws InterruptedException {
        return writeQueue.take();
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
