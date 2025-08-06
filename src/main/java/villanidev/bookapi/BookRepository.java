package villanidev.bookapi;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public class BookRepository {
    private final Map<String, Book> books = new ConcurrentHashMap<>();

    public List<Book> findAll() {
        return new ArrayList<>(books.values());
    }

    public Optional<Book> findById(String id) {
        return Optional.ofNullable(books.get(id));
    }

    public Book save(Book book) {
        String id = book.id() == null ? UUID.randomUUID().toString() : book.id();
        Book newBook = new Book(id, book.title(), book.author(), book.year());
        books.put(id, newBook);
        return newBook;
    }

    public boolean delete(String id) {
        return books.remove(id) != null;
    }
}
