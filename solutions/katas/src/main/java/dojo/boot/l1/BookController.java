package dojo.boot.l1;

import dojo.boot.l1.BookStore.Book;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reference solution. ResponseEntity lets you choose status and headers; plain returns mean 200. */
@RestController
@RequestMapping("/api/books")
public class BookController {

	public record NewBook(String title, String author, long priceCents) {
	}

	private final BookStore store;

	public BookController(BookStore store) {
		this.store = store;
	}

	@GetMapping
	public List<Book> all() {
		return store.all();
	}

	@GetMapping("/{id}")
	public ResponseEntity<Book> one(@PathVariable long id) {
		return ResponseEntity.of(store.find(id)); // empty Optional -> 404
	}

	@PostMapping
	public ResponseEntity<Book> create(@RequestBody NewBook body) {
		Book book = store.add(body.title(), body.author(), body.priceCents());
		return ResponseEntity.created(URI.create("/api/books/" + book.id())).body(book);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable long id) {
		return store.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}

}
