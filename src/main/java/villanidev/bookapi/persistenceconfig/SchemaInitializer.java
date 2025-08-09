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
                    pub_year INTEGER
                )
                """);

            // Create indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_books_title ON books(title)");

        } catch (SQLException e) {
            throw new RuntimeException("Schema initialization failed", e);
        }
    }
}
