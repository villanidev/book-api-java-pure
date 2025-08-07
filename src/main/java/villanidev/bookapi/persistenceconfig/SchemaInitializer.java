package villanidev.bookapi.persistenceconfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaInitializer {
    public static void initializeSchema(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Main table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS books (
                    id VARCHAR(36) PRIMARY KEY,
                    title VARCHAR(255),
                    author VARCHAR(255),
                    pub_year INTEGER,
                    version BIGINT,
                    last_updated TIMESTAMP
                )
                """);

            // Event store
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS book_events (
                    event_id VARCHAR(36),
                    book_id VARCHAR(36),
                    type VARCHAR(20),
                    data JSON,
                    version BIGINT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (event_id),
                    FOREIGN KEY (book_id) REFERENCES books(id)
                )
                """);

            // Create indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_books_version ON books(version)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_book_events_book_id ON book_events(book_id)");

        } catch (SQLException e) {
            throw new RuntimeException("Schema initialization failed", e);
        }
    }
}
