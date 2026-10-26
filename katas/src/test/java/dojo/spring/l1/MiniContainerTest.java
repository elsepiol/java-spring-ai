package dojo.spring.l1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dojo.spring.l1.MiniContainer.BeanCreationException;
import dojo.spring.l1.MiniContainer.NoSuchBeanException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("spring")
@Tag("L1")
class MiniContainerTest {

	public interface ProductRepository {
		String find(long id);
	}

	public static class InMemoryProductRepository implements ProductRepository {
		public InMemoryProductRepository() {
		}

		@Override
		public String find(long id) {
			return "Mug";
		}
	}

	public static class JdbcProductRepository implements ProductRepository {
		public JdbcProductRepository() {
		}

		@Override
		public String find(long id) {
			return "from the database";
		}
	}

	public static class PriceCalculator {
		public PriceCalculator() {
		}
	}

	public static class ProductService {
		final ProductRepository repository;
		final PriceCalculator prices;

		public ProductService(ProductRepository repository, PriceCalculator prices) {
			this.repository = repository;
			this.prices = prices;
		}
	}

	public static class OrderService {
		final ProductService products;
		final PriceCalculator prices;

		public OrderService(ProductService products, PriceCalculator prices) {
			this.products = products;
			this.prices = prices;
		}
	}

	public static class A {
		public A(B b) {
		}
	}

	public static class B {
		public B(C c) {
		}
	}

	public static class C {
		public C(A a) {
		}
	}

	@Test
	void injectsConstructorDependenciesRecursively() {
		MiniContainer c = new MiniContainer().register(OrderService.class, ProductService.class, PriceCalculator.class, InMemoryProductRepository.class);
		OrderService orders = c.get(OrderService.class);
		assertThat(orders.products.repository.find(7)).isEqualTo("Mug");
	}

	@Test
	void beansAreSingletonsAndShared() {
		MiniContainer c = new MiniContainer().register(OrderService.class, ProductService.class, PriceCalculator.class, InMemoryProductRepository.class);
		OrderService orders = c.get(OrderService.class);
		assertThat(c.get(OrderService.class)).isSameAs(orders);
		assertThat(orders.prices).isSameAs(orders.products.prices).isSameAs(c.get(PriceCalculator.class));
	}

	@Test
	void anInterfaceResolvesToItsOnlyImplementation() {
		MiniContainer c = new MiniContainer().register(InMemoryProductRepository.class);
		assertThat(c.get(ProductRepository.class)).isInstanceOf(InMemoryProductRepository.class).isSameAs(c.get(InMemoryProductRepository.class));
	}

	@Test
	void aMissingDependencyIsNamed() {
		MiniContainer c = new MiniContainer().register(ProductService.class, PriceCalculator.class);
		assertThatThrownBy(() -> c.get(ProductService.class)).isInstanceOf(NoSuchBeanException.class).hasMessageContaining("ProductRepository");
	}

	@Test
	void twoCandidatesAreAmbiguous() {
		MiniContainer c = new MiniContainer().register(InMemoryProductRepository.class, JdbcProductRepository.class);
		assertThatThrownBy(() -> c.get(ProductRepository.class)).isInstanceOf(BeanCreationException.class)
				.hasMessageContainingAll("InMemoryProductRepository", "JdbcProductRepository");
	}

	@Test
	void aCycleIsReportedAsAPath() {
		MiniContainer c = new MiniContainer().register(A.class, B.class, C.class);
		assertThatThrownBy(() -> c.get(A.class)).isInstanceOf(BeanCreationException.class).hasMessageContaining("A -> B -> C -> A");
	}

}
