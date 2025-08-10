package villanidev.bookapi.messaging;

import villanidev.bookapi.Book;
import villanidev.bookapi.BookRepository;
import villanidev.bookapi.BookRepositoryWithCache;

public class QueueConsumer implements Runnable {
    private final QueueProducer producer;
    private final BookRepositoryWithCache repository;
    private volatile boolean running = true;

    public QueueConsumer(QueueProducer producer, BookRepositoryWithCache repository) {
        this.producer = producer;
        this.repository = repository;
    }

    @Override
    public void run() {
        System.out.println("Consumer is running "+ Thread.currentThread().getName());
        while (running) {
            try {
                Book book = producer.take();
                System.out.println("Consumed from queue: "+ Thread.currentThread().getName() + ", "+ book.toString());
                repository.save(book);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println(e);
            }
        }
    }

    public void stop() {
        running = false;
    }
}
