package villanidev.bookapi;

import net.openhft.chronicle.map.ChronicleMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ChronicleBookRepository {
    private final ChronicleMap<String, Book> bookMap;
    private final Path storagePath;

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
    }

    public Book save(Book book) {
        String id = book.id() == null ? UUID.randomUUID().toString() : book.id();
        Book newBook = new Book(id, book.title(), book.author(), book.year());
        bookMap.put(id, newBook);
        return newBook;
    }

    public Optional<Book> findById(String id) {
        return Optional.ofNullable(bookMap.get(id));
    }

    public List<Book> findAll() {
        System.out.println("find all");
        return new ArrayList<>(bookMap.values());
    }

    public boolean delete(String id) {
        return bookMap.remove(id) != null;
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
}
