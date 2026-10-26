package dojo.javacore.l1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("javacore")
@Tag("L1")
class MoneyTest {

	@Test
	void holdsCentsAndAnUpperCaseCurrency() {
		Money m = Money.of(1250, "eur");
		assertThat(m.cents()).isEqualTo(1250);
		assertThat(m.currency()).isEqualTo("EUR");
	}

	@Test
	void plusReturnsANewValueAndLeavesBothOperandsAlone() {
		Money a = Money.of(1000, "EUR");
		Money b = Money.of(250, "EUR");
		Money sum = a.plus(b);
		assertThat(sum.cents()).isEqualTo(1250);
		assertThat(a.cents()).as("immutable").isEqualTo(1000);
		assertThat(sum).isNotSameAs(a);
	}

	@Test
	void equalValuesAreEqualAndHashTheSame() {
		Money a = Money.of(500, "USD");
		Money b = Money.of(500, "usd");
		assertThat(a).isEqualTo(b).isNotSameAs(b);
		assertThat(a.hashCode()).isEqualTo(b.hashCode());
		assertThat(a).isNotEqualTo(Money.of(500, "EUR")).isNotEqualTo(Money.of(501, "USD")).isNotEqualTo(null).isNotEqualTo("500 USD");
	}

	@Test
	void worksAsAHashMapKeyAndInAHashSet() {
		Set<Money> set = new HashSet<>();
		set.add(Money.of(100, "EUR"));
		set.add(Money.of(100, "EUR"));
		assertThat(set).hasSize(1);

		Map<Money, String> labels = new HashMap<>();
		labels.put(Money.of(999, "EUR"), "cheap");
		assertThat(labels.get(Money.of(999, "EUR"))).isEqualTo("cheap");
	}

	@Test
	void refusesToMixCurrencies() {
		assertThatThrownBy(() -> Money.of(1, "EUR").plus(Money.of(1, "USD"))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void refusesAMissingCurrency() {
		assertThatThrownBy(() -> Money.of(1, null)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Money.of(1, "  ")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void printsTwoDecimals() {
		assertThat(Money.of(1250, "EUR")).hasToString("12.50 EUR");
		assertThat(Money.of(7, "EUR")).hasToString("0.07 EUR");
		assertThat(Money.of(-5, "EUR")).hasToString("-0.05 EUR");
		assertThat(Money.of(100000, "JPY")).hasToString("1000.00 JPY");
	}

}
