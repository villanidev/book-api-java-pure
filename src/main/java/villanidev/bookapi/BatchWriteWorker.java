package villanidev.bookapi;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class BatchWriteWorker implements Runnable {
    private final BlockingQueue<Book> writeQueue;
    private final int batchSize = 200; //chunk size
    private final Duration batchSleep = Duration.ofMillis(50);
    private final BookRepositoryWithCache repository;

    public BatchWriteWorker(BlockingQueue<Book> writeQueue, BookRepositoryWithCache repository) {
        this.writeQueue = writeQueue;
        this.repository = repository;
    }

    public void run() {
        List<Book> batchRecords = new ArrayList<>(batchSize);
        while (true) {
            System.out.println("batch worker running: " + Thread.currentThread().getName());
            try {
                // Wait for first event
                Book firstEvent = writeQueue.take();
                batchRecords.add(firstEvent);

                // Drain remaining events with timeout
                writeQueue.drainTo(batchRecords, batchSize - 1);

                // If batch isn't full, wait briefly for more
                if (batchRecords.size() < batchSize) {
                    Thread.sleep(batchSleep.toMillis());
                    writeQueue.drainTo(batchRecords, batchSize - batchRecords.size());
                }

                processBatch(batchRecords);
                batchRecords.clear();
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processBatch(List<Book> batch) {
        System.out.println("processBatch: " + Thread.currentThread().getName());
        repository.saveBooks(batch);
    }
}
