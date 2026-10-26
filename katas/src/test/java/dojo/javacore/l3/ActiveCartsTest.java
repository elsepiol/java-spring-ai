package dojo.javacore.l3;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("javacore")
@Tag("L3")
class ActiveCartsTest {

	@Test
	void aRenamedCartCanBeFoundUnderItsNewSession() {
		ActiveCarts carts = new ActiveCarts();
		ActiveCarts.Cart cart = carts.open(7, "s-old");
		cart.items.add("mug");

		carts.renameSession("s-old", "s-new");

		assertThat(carts.find(7, "s-new")).containsSame(cart);
		assertThat(carts.find(7, "s-old")).isEmpty();
	}

	@Test
	void closingARenamedCartFreesIt() {
		ActiveCarts carts = new ActiveCarts();
		carts.open(7, "s-old");
		carts.renameSession("s-old", "s-new");

		carts.close(7, "s-new");

		assertThat(carts.size()).as("nothing may stay behind after close").isZero();
	}

	@Test
	void aDayOfTrafficLeavesNothingBehind() {
		ActiveCarts carts = new ActiveCarts();
		for (int user = 0; user < 10_000; user++) {
			carts.open(user, "a" + user);
			carts.renameSession("a" + user, "b" + user); // logged in again
			carts.close(user, "b" + user); // checked out
		}
		assertThat(carts.size()).isZero();
	}

}
