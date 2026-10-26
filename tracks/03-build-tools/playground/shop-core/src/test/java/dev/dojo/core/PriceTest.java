package dev.dojo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PriceTest {

	@Test
	void addsVat() {
		assertEquals(new Price(2419), new Price(1999).withVat(21));
	}

	@Test
	void rejectsNegativePrices() {
		assertThrows(IllegalArgumentException.class, () -> new Price(-1));
	}

}
