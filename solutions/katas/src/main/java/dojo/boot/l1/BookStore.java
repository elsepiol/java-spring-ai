package dojo.boot.l1;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** Given: an in-memory "repository". Not a kata. */
@Component
public class BookStore {

	public record Book(long id, String title, String author, long priceCents) {
	}

	private final Map<Long, Book> books = new ConcurrentHashMap<>();
	private final AtomicLong ids = new AtomicLong();

	public Book add(String title, String author, long priceCents) {
		Book book = new Book(ids.incrementAndGet(), title, author, priceCents);
		books.put(book.id(), book);
		return book;
	}

	public Optional<Book> find(long id) {
		return Optional.ofNullable(books.get(id));
	}

	public List<Book> all() {
		return books.values().stream().sorted(Comparator.comparingLong(Book::id)).toList();
	}

	public boolean delete(long id) {
		return books.remove(id) != null;
	}

}
