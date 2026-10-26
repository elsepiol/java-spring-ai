package dojo.jpa.l3.locking;

import org.springframework.stereotype.Service;

/**
 * KATA jpa L3 "OptimisticLocking" part 2. save() stores an edited (detached) product, as the update
 * endpoint would after reading the form. If someone else saved in between, throw ConcurrentEditException
 * (the API maps it to 409/412) instead of letting Spring's locking exception escape.
 */
@Service
public class ProductEditor {

	public static class ConcurrentEditException extends RuntimeException {
		public ConcurrentEditException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private final EditableProductRepository products;

	public ProductEditor(EditableProductRepository products) {
		this.products = products;
	}

	public EditableProduct save(EditableProduct edited) {
		return products.save(edited);
	}

}
