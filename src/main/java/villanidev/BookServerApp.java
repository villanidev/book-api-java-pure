package villanidev;

import villanidev.bookapi.BookController;
import villanidev.bookapi.BookRepository;
import villanidev.httpserver.JServer;

import java.io.IOException;

public class BookServerApp {
    public static void main(String[] args) throws IOException {
        BookController bookController = new BookController(new BookRepository());

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