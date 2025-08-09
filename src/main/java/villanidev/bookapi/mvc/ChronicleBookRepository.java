package villanidev.bookapi.mvc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.openhft.chronicle.map.ChronicleMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChronicleBookRepository {
    private final ChronicleMap<String, Book> bookMap;
    private final Path storagePath;
    private final Cache<String, Book> cache;

    List<Lock> locks;

    public ChronicleBookRepository() throws IOException {
        this.storagePath = getStoragePath();
        ensureDirectoryExists(storagePath.getParent());

        this.bookMap = ChronicleMap
                .of(String.class, Book.class)
                .name("books-map")
                .entries(50_000) // Expected max entries
                .actualSegments(Runtime.getRuntime().availableProcessors()) // Match CPU cores
                .averageKeySize(36) // UUID size
                .averageValueSize(200) // Average book size
                .putReturnsNull(true) // Slightly faster puts
                .removeReturnsNull(true) // Slightly faster removes
                .createPersistedTo(storagePath.toFile());

        this.cache = Caffeine.newBuilder()
                .maximumSize(50_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(bookMap::get);

        this.locks = Stream.generate(ReentrantLock::new)
                .limit(16)  // Number of stripes
                .collect(Collectors.toList());

    }

    public void save(Book book) {
        /*synchronized (this) {  // Global lock for simplicity
            String id = book.id() == null ? UUID.randomUUID().toString() : book.id();
            Book newBook = new Book(id, book.title(), book.author(), book.year());
            bookMap.put(id, newBook);
            cache.put(id, newBook);
        }*/

        String id = book.id() == null ? UUID.randomUUID().toString() : book.id();
        Book newBook = new Book(id, book.title(), book.author(), book.year());
        Lock lock = getLock(id);
        lock.lock();
        try {
            bookMap.put(id, newBook);
            cache.put(id, newBook);
        } finally {
            lock.unlock();
        }
    }

    public Optional<Book> findById(String id) {
        System.out.println("find by : " + id);
        return Optional.ofNullable(cache.getIfPresent(id));
    }

    public List<Book> findAll() {
        System.out.println("find all");
        return cache.asMap().values().stream().toList();
    }

    public boolean delete(String id) {
        boolean removed = bookMap.remove(id) != null;
        if (removed) {
            this.cache.invalidate(id);
        }
        return removed;
    }

    public void close() {
        bookMap.close();
    }

    private Path getStoragePath() {
        // Try application directory first
        Path appDirPath = Path.of("data/books.dat");
        if (Files.isWritable(appDirPath.getParent())) {
            return appDirPath;
        }

        // Fallback to system temp directory
        return Path.of(System.getProperty("java.io.tmpdir"), "books.dat");
    }

    private void ensureDirectoryExists(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    public Cache<String, Book> getCache() {
        return cache;
    }

    private Lock getLock(String key) {
        return locks.get(Math.abs(key.hashCode() % locks.size()));
    }
}
