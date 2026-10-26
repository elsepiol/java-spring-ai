package dev.dojo.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MainTest {

	@Test
	void totalIncludesVat() {
		assertEquals("24.19", Main.checkoutTotal(1999).toString());
	}

}
