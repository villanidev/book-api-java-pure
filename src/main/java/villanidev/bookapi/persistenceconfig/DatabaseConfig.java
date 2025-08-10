package villanidev.bookapi.persistenceconfig;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DatabaseConfig {
    private static final Path DB_PATH = Paths.get("./data/bookstore");
    private static final int MAX_POOL_SIZE = 20;
    public static final int MIN_IDLE = 10;
    public static final long VALIDATION_TIMEOUT = 1000L;
    public static final long CONNECTION_TIMEOUT = 3000L;

    public static DataSource createDataSource() {
        // Ensure database directory exists
        DB_PATH.toFile().mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:file:" + DB_PATH + "/books;DB_CLOSE_ON_EXIT=FALSE");
        config.setUsername("");
        config.setPassword("");
        config.setMinimumIdle(MIN_IDLE);
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setValidationTimeout(VALIDATION_TIMEOUT);
        config.setConnectionTimeout(CONNECTION_TIMEOUT);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "10");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "20");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("wal", "true");
        config.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(config);
    }
}
