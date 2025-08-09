package villanidev;

import com.zaxxer.hikari.HikariDataSource;
import villanidev.bookapi.mvc.BookController;
import villanidev.bookapi.mvc.BookService;
import villanidev.bookapi.persistenceconfig.DatabaseConfig;
import villanidev.bookapi.persistenceconfig.SchemaInitializer;
import villanidev.bookapi.mvc.BookRepositoryWithCache;
import villanidev.httpserver.JServer;
import villanidev.httpserver.SimpleAsyncExecutor;

import java.io.IOException;

public class BookServerApp {

    public static void main(String[] args) throws IOException {

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
    }
}