package dojo.sql.l1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import dojo.sql.support.ShopDatabase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("sql")
@Tag("L1")
class ProductSearchTest extends ShopDatabase {

	@Test
	void findsByPartOfTheNameIgnoringCase() throws Exception {
		assertThat(ProductSearch.skusMatching(db, "MU")).containsExactly("MUG-1");
		assertThat(ProductSearch.skusMatching(db, "n")).containsExactly("KNF-1", "PAN-1", "PEN-1");
	}

	@Test
	void anAttackerCannotListEveryProduct() throws Exception {
		assertThat(ProductSearch.skusMatching(db, "zzz' OR 1=1 --")).isEmpty();
	}

	@Test
	void anAttackerCannotRunTheirOwnStatements() throws Exception {
		assertThatCode(() -> ProductSearch.skusMatching(db, "x'; UPDATE products SET price_cents = 1; --")).doesNotThrowAnyException();
		assertThat(query("SELECT price_cents FROM products WHERE sku = 'DSK-1'")).containsExactly(row(19900));
	}

	@Test
	void anApostropheInTheSearchIsJustText() throws Exception {
		assertThatCode(() -> ProductSearch.skusMatching(db, "Dan's mug")).doesNotThrowAnyException();
	}

	@Test
	void wildcardsTypedByTheUserMatchLiterally() throws Exception {
		assertThat(ProductSearch.skusMatching(db, "%")).isEmpty();
		assertThat(ProductSearch.skusMatching(db, "_")).isEmpty();
	}

}
