package villanidev.bookapi.messaging;

import villanidev.bookapi.Book;
import villanidev.httpserver.JHttpRequest;
import villanidev.httpserver.JHttpResponse;
import villanidev.httpserver.utils.JsonUtils;

public class BookController2 {
    private final BookService2 bookService;

    public BookController2(BookService2 bookService) {
        this.bookService = bookService;
    }

    public void createBook(JHttpRequest req, JHttpResponse res) {
        try {
            System.out.println("Received request: "+ Thread.currentThread().getName());
            Book book = JsonUtils.fromJson(req.getBody(), Book.class);
            bookService.createBook(book)
                    .thenAccept(v -> res.status(201).json(book))
                    .exceptionally(ex -> {
                        res.status(503).send("Service unavailable");
                        return null;
                    });
        } catch (Exception e) {
            res.status(400).send("Invalid request");
        }
    }

    public void getBook(JHttpRequest req, JHttpResponse res) {
        bookService.getBook(req.getPathParam("id"))
                .ifPresentOrElse(
                        res::json,
                        () -> res.status(404).send("Not found")
                );
    }
}
