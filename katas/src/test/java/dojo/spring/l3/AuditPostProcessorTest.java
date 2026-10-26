package dojo.spring.l3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Tag("spring")
@Tag("L3")
class AuditPostProcessorTest {

	public interface Refunds {
		String refund(long orderId);
	}

	public interface Catalog {
		String name(long id);
	}

	@Audited
	public static class RefundService implements Refunds {
		@Override
		public String refund(long orderId) {
			if (orderId < 0) {
				throw new IllegalArgumentException("no such order");
			}
			return "refunded " + orderId;
		}
	}

	public static class CatalogService implements Catalog {
		@Override
		public String name(long id) {
			return "Mug";
		}
	}

	public static class Support {
		final Refunds refunds;

		Support(Refunds refunds) {
			this.refunds = refunds;
		}
	}

	@Configuration
	static class Config {
		@Bean
		static AuditLog auditLog() {
			return new AuditLog();
		}

		@Bean
		static AuditPostProcessor auditPostProcessor(AuditLog log) { // static: post-processors are created early
			return new AuditPostProcessor(log);
		}

		@Bean
		Refunds refunds() {
			return new RefundService();
		}

		@Bean
		Catalog catalog() {
			return new CatalogService();
		}

		@Bean
		Support support(Refunds refunds) {
			return new Support(refunds);
		}
	}

	@Test
	void auditedBeansAreWrappedEverywhereTheyAreInjected() {
		try (var context = new AnnotationConfigApplicationContext(Config.class)) {
			Support support = context.getBean(Support.class);
			assertThat(support.refunds.refund(7)).isEqualTo("refunded 7");
			assertThat(context.getBean(AuditLog.class).entries).containsExactly("refunds.refund");
			assertThat(support.refunds).isNotInstanceOf(RefundService.class);
		}
	}

	@Test
	void otherBeansAreUntouched() {
		try (var context = new AnnotationConfigApplicationContext(Config.class)) {
			assertThat(context.getBean(Catalog.class)).isInstanceOf(CatalogService.class);
			context.getBean(Catalog.class).name(1);
			assertThat(context.getBean(AuditLog.class).entries).isEmpty();
		}
	}

	@Test
	void exceptionsPassThroughUnchanged() {
		try (var context = new AnnotationConfigApplicationContext(Config.class)) {
			assertThatThrownBy(() -> context.getBean(Refunds.class).refund(-1)).isExactlyInstanceOf(IllegalArgumentException.class);
		}
	}

}
