package villanidev.bookapi.persistenceconfig;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DatabaseConfig {
    private static final Path DB_PATH = Paths.get("./data/bookstore");
    private static final int MAX_POOL_SIZE = 20;

    public static DataSource createDataSource() {
        // Ensure database directory exists
        DB_PATH.toFile().mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:file:" + DB_PATH + "/books;DB_CLOSE_ON_EXIT=FALSE");
        config.setUsername("");
        config.setPassword("");
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setConnectionTimeout(3000L);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("wal", "true");
        return new HikariDataSource(config);
    }
}
