package villanidev;

import villanidev.bookapi.BookController;
import villanidev.bookapi.BookRepository;
import villanidev.bookapi.ChronicleBookRepository;
import villanidev.httpserver.JServer;

import java.io.IOException;

public class BookServerApp {

    public static void main(String[] args) throws IOException {
        //ChronicleBookRepository repository = new ChronicleBookRepository();
        // Add shutdown hook to properly close the map
        //Runtime.getRuntime().addShutdownHook(new Thread(repository::close));

        BookRepository repository = new BookRepository();
        BookController bookController = new BookController(repository);

        JServer server = new JServer.Builder()
                .port(8080)
                .addRoute("GET", "/books", bookController::listBooks)
                .addRoute("GET", "/books/:id", bookController::getBook)
                .addRoute("POST", "/books", bookController::createBook)
                .addRoute("PUT", "/books/:id", bookController::updateBook)
                .addRoute("DELETE", "/books/:id", bookController::deleteBook)
                .addRoute("GET", "/health", bookController::healthCheck)
                .build();

        server.start();
    }
}