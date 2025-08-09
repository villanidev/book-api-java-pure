package villanidev.bookapi.batchworker;

import com.github.benmanes.caffeine.cache.LoadingCache;
import villanidev.bookapi.events.BookEvent;
import villanidev.bookapi.events.VersionedBook;
import villanidev.httpserver.utils.JsonUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class BatchWriteWorker implements Runnable {
    private final BlockingQueue<BookEvent> writeQueue;
    private final DataSource dataSource;
    private final LoadingCache<String, VersionedBook> cache;
    private final int batchSize = 200; //chunk size
    private final Duration batchSleep = Duration.ofMillis(50);

    public BatchWriteWorker(BlockingQueue<BookEvent> writeQueue,
                            DataSource dataSource,
                            LoadingCache<String, VersionedBook> cache) {
        this.writeQueue = writeQueue;
        this.dataSource = dataSource;
        this.cache = cache;
    }

    public void run() {
        List<BookEvent> batchRecords = new ArrayList<>(batchSize);
        while (true) {
            System.out.println("batch worker running: " + Thread.currentThread().getName());
            try {
                // Wait for first event
                BookEvent firstEvent = writeQueue.take();
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

    private void processBatch(List<BookEvent> batch) throws Exception {
        System.out.println("processBatch: " + Thread.currentThread().getName());
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Update books table
            try (PreparedStatement stmt = conn.prepareStatement(
                    "MERGE INTO books KEY(id) VALUES (?, ?, ?, ?, ?, ?)")) {
                for (BookEvent event : batch) {
                    VersionedBook versioned = new VersionedBook(
                            event.book(),
                            event.version(),
                            event.timestamp()
                    );
                    stmt.setString(1, event.bookId());
                    stmt.setString(2, versioned.book().title());
                    stmt.setString(3, versioned.book().author());
                    stmt.setInt(4, versioned.book().year());
                    stmt.setLong(5, versioned.version());
                    stmt.setTimestamp(6, Timestamp.from(versioned.lastUpdated()));
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            // 2. Update cache
            batch.forEach(event -> {
                VersionedBook versioned = new VersionedBook(
                        event.book(),
                        event.version(),
                        event.timestamp()
                );
                cache.put(event.bookId(), versioned);
            });

            conn.commit();
        }
    }
}
