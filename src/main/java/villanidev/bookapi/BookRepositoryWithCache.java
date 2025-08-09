package villanidev.bookapi;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class BookRepositoryWithCache {
    private final LoadingCache<String, Book> cache;
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
                     "SELECT id, title, author, pub_year FROM books")) {
            while (rs.next()) {
                Book book = new Book(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("pub_year")
                );
                System.out.println(book);
                cache.put(book.id(), book);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cache warm-up failed", e);
        }
    }

    public LoadingCache<String, Book> getCache() {
        return cache;
    }

    public Book save(Book book) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Save to main table
            try (PreparedStatement stmt = conn.prepareStatement(
                    "MERGE INTO books KEY(id) VALUES (?, ?, ?, ?)")) {
                stmt.setString(1, book.id());
                stmt.setString(2, book.title());
                stmt.setString(3, book.author());
                stmt.setInt(4, book.year());
                stmt.executeUpdate();
            }

            // 3. Update cache
            cache.put(book.id(), book);
            conn.commit();
        } catch (Exception e) {
            throw new RuntimeException("Error while saving book: ", e);
        }

        return book;
    }

    public void saveBooks(List<Book> books) {
        try {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);

                // 1. Update books table
                try (PreparedStatement stmt = conn.prepareStatement(
                        "MERGE INTO books KEY(id) VALUES (?, ?, ?, ?)")) {
                    for (Book event : books) {
                        stmt.setString(1, event.id());
                        stmt.setString(2, event.title());
                        stmt.setString(3, event.author());
                        stmt.setInt(4, event.year());
                        stmt.executeUpdate();
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                // 2. Update cache
                books.forEach(event -> cache.put(event.id(), event));
                conn.commit();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while saving books: ", e);
        }
    }

    public Optional<Book> findById(String id) {
        System.out.println("find by : " + id);
        return Optional.ofNullable(cache.get(id));
    }

    public List<Book> findAll() {
        System.out.println("find all");
        return cache.asMap().values().stream().toList();
    }

    private Book loadFromDb(String id) {
        System.out.println("loadFromDb: " + Thread.currentThread().getName());
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, title, author, pub_year FROM books WHERE id = ?")) {

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
                    return book;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load book from database", e);
        }
        return null;
    }
}
