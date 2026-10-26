package dojo.spring.l3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("spring")
@Tag("L3")
class TimingProxyTest {

	public interface Inventory {
		int stock(String sku);

		void reserve(String sku, int qty) throws IOException;
	}

	static class RealInventory implements Inventory {
		@Override
		public int stock(String sku) {
			return sku.equals("MUG-1") ? 50 : 0;
		}

		@Override
		public void reserve(String sku, int qty) throws IOException {
			if (qty > 50) {
				throw new IOException("warehouse unreachable");
			}
			if (qty <= 0) {
				throw new IllegalArgumentException("qty must be positive");
			}
		}

		@Override
		public String toString() {
			return "RealInventory";
		}
	}

	private final List<String> log = new ArrayList<>();
	private final Inventory inventory = TimingProxy.wrap(new RealInventory(), Inventory.class, log);

	@Test
	void delegatesAndRecords() throws Exception {
		assertThat(inventory.stock("MUG-1")).isEqualTo(50);
		inventory.reserve("MUG-1", 2);
		assertThat(log).containsExactly("stock ok", "reserve ok");
	}

	@Test
	void isAProxyNotTheTarget() {
		assertThat(inventory).isNotInstanceOf(RealInventory.class).isInstanceOf(Inventory.class);
		assertThat(java.lang.reflect.Proxy.isProxyClass(inventory.getClass())).isTrue();
	}

	@Test
	void rethrowsTheOriginalUncheckedException() {
		assertThatThrownBy(() -> inventory.reserve("MUG-1", 0)).isExactlyInstanceOf(IllegalArgumentException.class);
		assertThat(log).containsExactly("reserve failed: IllegalArgumentException");
	}

	@Test
	void rethrowsTheOriginalCheckedException() {
		assertThatThrownBy(() -> inventory.reserve("MUG-1", 99)).isExactlyInstanceOf(IOException.class).hasMessage("warehouse unreachable");
	}

	@Test
	void objectMethodsAreDelegatedButNotRecorded() {
		assertThat(inventory.toString()).isEqualTo("RealInventory");
		assertThat(log).isEmpty();
	}

}
