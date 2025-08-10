package villanidev.bookapi.messaging;

import villanidev.bookapi.Book;
import villanidev.bookapi.BookRepositoryWithCache;
import villanidev.bookapi.messaging.QueueProducer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BookService2 {
    private final QueueProducer producer;
    private final BookRepositoryWithCache repository;

    public BookService2(QueueProducer producer, BookRepositoryWithCache repository) {
        this.producer = producer;
        this.repository = repository;
    }

    public CompletableFuture<Void> createBook(Book book) {
        System.out.println("service2 " + Thread.currentThread().getName());
        String id = book.id() == null ? UUID.randomUUID().toString() : book.id();
        Book newBook = new Book(id, book.title(), book.author(), book.year());
        return producer.enqueue(newBook);
    }

    public Optional<Book> getBook(String id) {
        System.out.println("findById " + Thread.currentThread().getName());
        return repository.findById(id);
    }
}
