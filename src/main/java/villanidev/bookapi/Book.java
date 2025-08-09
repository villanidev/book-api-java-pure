package villanidev.bookapi;

import java.io.Serial;
import java.io.Serializable;

public record Book(String id, String title, String author, int year) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
