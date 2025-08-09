package villanidev;

import com.zaxxer.hikari.HikariDataSource;
import villanidev.bookapi.BookController;
import villanidev.bookapi.BookRepositoryWithCache;
import villanidev.bookapi.BookService;
import villanidev.bookapi.persistenceconfig.DatabaseConfig;
import villanidev.bookapi.persistenceconfig.SchemaInitializer;
import villanidev.httpserver.JServer;
import villanidev.httpserver.SimpleAsyncExecutor;

import java.time.Duration;
import java.time.Instant;

public class BookServerApp {

    public static void main(String[] args) {
        try {
            Instant start = Instant.now();

            // Initialize database
            HikariDataSource dataSource = (HikariDataSource) DatabaseConfig.createDataSource();
            SchemaInitializer.initializeSchema(dataSource);

            // DI
            BookRepositoryWithCache repository = new BookRepositoryWithCache(dataSource);
            BookService service = new BookService(repository);
            // Create executor (uses virtual threads by default)
            SimpleAsyncExecutor asyncExecutor = new SimpleAsyncExecutor();
            BookController bookController = new BookController(service, asyncExecutor);

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(dataSource::close));

            // Create and start server
            JServer server = new JServer.Builder()
                    .port(8080)
                    .addRoute("GET", "/books", bookController::listBooks)
                    .addRoute("GET", "/books/:id", bookController::getBook)
                    .addRoute("POST", "/books", bookController::createBook)
                    //.addRoute("PUT", "/books/:id", bookController::updateBook)
                    //.addRoute("DELETE", "/books/:id", bookController::deleteBook)
                    .addRoute("GET", "/health", bookController::healthCheck)
                    .addRoute("GET", "/ping", bookController::ping)
                    .build();

            server.start();

            Instant finish = Instant.now();
            long timeElapsed = Duration.between(start, finish).toMillis();
            System.out.println("Server started in " + timeElapsed + " (ms)");
        } catch (Exception e) {
            throw new RuntimeException("App initialization error: ", e);
        }
    }
}