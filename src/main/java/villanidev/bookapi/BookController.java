package villanidev.bookapi;

import villanidev.httpserver.JHttpRequest;
import villanidev.httpserver.JHttpResponse;
import villanidev.httpserver.utils.JsonUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class BookController {
    private final BookRepository repository;

    public BookController(BookRepository repository) {
        this.repository = repository;
    }

    public void listBooks(JHttpRequest request, JHttpResponse response) {
        response.json(repository.findAll());
    }

    public void getBook(JHttpRequest request, JHttpResponse response) {
        String id = request.getPath().split("/")[2];
        repository.findById(id).ifPresentOrElse(
                response::json,
                () -> response.status(404).send("Book not found")
        );
    }

    public void createBook(JHttpRequest request, JHttpResponse response) {
        try {
            Book book = JsonUtils.fromJson(request.getBody(), Book.class);
            Book savedBook = repository.save(book);
            response.status(201).json(savedBook);
        } catch (Exception e) {
            response.status(400).send("Invalid book data");
        }
    }

    public void updateBook(JHttpRequest request, JHttpResponse response) {
        String id = request.getPath().split("/")[2];
        if (repository.findById(id).isEmpty()) {
            response.status(404).send("Book not found");
            return;
        }

        try {
            Book book = JsonUtils.fromJson(request.getBody(), Book.class);
            Book updatedBook = repository.save(new Book(id, book.title(), book.author(), book.year()));
            response.json(updatedBook);
        } catch (Exception e) {
            response.status(400).send("Invalid book data");
        }
    }

    public void deleteBook(JHttpRequest request, JHttpResponse response) {
        String id = request.getPath().split("/")[2];
        if (repository.delete(id)) {
            response.status(204).send("");
        } else {
            response.status(404).send("Book not found");
        }
    }

    public void healthCheck(JHttpRequest request, JHttpResponse response) {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        response.json(health);
    }
}
