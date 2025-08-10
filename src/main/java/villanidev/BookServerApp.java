package villanidev;

import com.zaxxer.hikari.HikariDataSource;
import villanidev.bookapi.*;
import villanidev.bookapi.messaging.BookController2;
import villanidev.bookapi.messaging.BookService2;
import villanidev.bookapi.messaging.QueueConsumer;
import villanidev.bookapi.messaging.QueueProducer;
import villanidev.bookapi.persistenceconfig.DatabaseConfig;
import villanidev.bookapi.persistenceconfig.SchemaInitializer;
import villanidev.httpserver.JServer;
import villanidev.httpserver.SimpleAsyncExecutor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

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

            //producer - consumer
            QueueProducer producer = new QueueProducer(10_000);
            BookService2 service2 = new BookService2(producer, repository);
            BookController2 controller2 = new BookController2(service2);

            // Start consumers
            int consumerCount = Runtime.getRuntime().availableProcessors();
            List<QueueConsumer> consumers = IntStream.range(0, consumerCount)
                    .mapToObj(i -> new QueueConsumer(producer, repository))
                    .toList();

            /*consumers.forEach(consumer ->
                    Thread.ofVirtual()
                            .name("consumer-", 0)
                            .start(consumer));*/

            for (int i = 0; i < consumers.size(); i++) {
                QueueConsumer consumer = consumers.get(i);
                Thread.ofVirtual()
                        .name("consumer-", i)
                        .start(consumer);
            }

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
                    .addRoute("POST", "/books2", controller2::createBook)
                    .addRoute("GET", "/books2/:id", controller2::getBook)
                    .build();

            server.start();

            Instant finish = Instant.now();
            long timeElapsed = Duration.between(start, finish).toMillis();
            System.out.println("Server started in " + timeElapsed + " (ms)");

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                dataSource.close();
                producer.shutdown();
                consumers.forEach(QueueConsumer::stop);
                asyncExecutor.shutdown();
            }));

        } catch (Exception e) {
            throw new RuntimeException("App initialization error: ", e);
        }
    }
}