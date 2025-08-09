package villanidev.bookapi;

import villanidev.httpserver.JHttpRequest;
import villanidev.httpserver.JHttpResponse;
import villanidev.httpserver.SimpleAsyncExecutor;
import villanidev.httpserver.utils.JsonUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class BookController {
   private final BookService service;
   private final SimpleAsyncExecutor asyncExecutor;

    public BookController(BookService service, SimpleAsyncExecutor simpleAsyncExecutor) {
        this.service = service;
        this.asyncExecutor = simpleAsyncExecutor;
    }

    public void listBooks(JHttpRequest request, JHttpResponse response) {
        response.json(service.findAll());
    }

    public void getBook(JHttpRequest request, JHttpResponse response) {
        String id = request.getPathParam("id");
        service.findById(id).ifPresentOrElse(
                response::json,
                () -> response.status(404).send("Book not found")
        );
    }

    public void createBook(JHttpRequest request, JHttpResponse response) {
        try {
            String body = request.getBody();
            asyncExecutor.execute(() -> {
                Book book = JsonUtils.fromJson(body, Book.class);
                service.save(book);
            });
            response.status(201).send(body, "application/json");
        } catch (Exception e) {
            response.status(400).send("Invalid book data");
        }
    }

    /*public void updateBook(JHttpRequest request, JHttpResponse response) {
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
    }*/

    /*public void deleteBook(JHttpRequest request, JHttpResponse response) {
        String id = request.getPath().split("/")[2];
        if (repository.delete(id)) {
            response.status(204).send("");
        } else {
            response.status(404).send("Book not found");
        }
    }*/

    public void healthCheck(JHttpRequest request, JHttpResponse response) {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        response.status(200).json(health);
    }

    public void ping(JHttpRequest request, JHttpResponse response) {
        response.status(200).send(null);
    }
}
