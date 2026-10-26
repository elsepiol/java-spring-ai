package dojo.jpa.l3.locking;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/** Reference solution: translate the technical exception into one the API layer understands. */
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
		try {
			return products.save(edited); // merge: compares the detached version with the row's
		} catch (ObjectOptimisticLockingFailureException e) {
			throw new ConcurrentEditException("product " + edited.getId() + " was changed by someone else", e);
		}
	}

}
