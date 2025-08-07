package villanidev.bookapi.mvc;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import villanidev.bookapi.events.VersionedBook;
import villanidev.httpserver.utils.JsonUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class BookRepositoryWithCache {
    private final LoadingCache<String, VersionedBook> cache;
    private final DataSource dataSource;

    public BookRepositoryWithCache(DataSource dataSource) {
        this.dataSource = dataSource;
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(this::loadFromDb);

        warmUpCache();
    }

    private void warmUpCache() {
        System.out.println("Cache warm up: " + Thread.currentThread().getName());
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT id, title, author, pub_year, version, last_updated FROM books")) {
            while (rs.next()) {
                Book book = new Book(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("pub_year")
                );
                VersionedBook versioned = new VersionedBook(
                        book,
                        rs.getLong("version"),
                        rs.getTimestamp("last_updated").toInstant()
                );
                System.out.println(versioned);
                cache.put(book.id(), versioned);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cache warm-up failed", e);
        }
    }

    public LoadingCache<String, VersionedBook> getCache() {
        return cache;
    }

    public CompletableFuture<Void> save(Book book) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        long version = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Save to main table
            try (PreparedStatement stmt = conn.prepareStatement(
                    "MERGE INTO books KEY(id) VALUES (?, ?, ?, ?, ?, ?)")) {
                stmt.setString(1, book.id());
                stmt.setString(2, book.title());
                stmt.setString(3, book.author());
                stmt.setInt(4, book.year());
                stmt.setLong(5, version);
                stmt.setTimestamp(6, Timestamp.from(Instant.now()));
                stmt.executeUpdate();
            }

            // 2. Save to audit log
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO book_events (event_id, book_id, type, data, version) " +
                            "VALUES (?, ?, ?, ?, ?)")) {
                stmt.setString(1, UUID.randomUUID().toString());
                stmt.setString(2, book.id());
                stmt.setString(3, "UPDATE");
                stmt.setString(4, JsonUtils.toJson(book));
                stmt.setLong(5, version);
                stmt.executeUpdate();
            }

            // 3. Update cache
            VersionedBook versionedBook = new VersionedBook(book, version, Instant.now());
            cache.put(book.id(), versionedBook);

            conn.commit();
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    public Optional<Book> findById(String id) {
        System.out.println("find by : " + id);
        Optional<VersionedBook> optional = Optional.ofNullable(cache.get(id));
        return optional.map(VersionedBook::book);
    }

    public Optional<VersionedBook> findById2(String id) {
        return Optional.ofNullable(cache.get(id));
    }

    public List<Book> findAll() {
        System.out.println("find all");
        return cache.asMap().values().stream().map(VersionedBook::book).toList();
    }

    private VersionedBook loadFromDb(String id) {
        System.out.println("loadFromDb: " + Thread.currentThread().getName());
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, title, author, pub_year, version, last_updated FROM books WHERE id = ?")) {

            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Book book = new Book(
                            rs.getString("id"),
                            rs.getString("title"),
                            rs.getString("author"),
                            rs.getInt("pub_year")
                    );
                    System.out.println(book);
                    return new VersionedBook(
                            book,
                            rs.getLong("version"),
                            rs.getTimestamp("last_updated").toInstant()
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load book from database", e);
        }
        return null;
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }
}
