package dojo.jpa.l3.locking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dojo.jpa.support.JpaKataTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Tag("jpa")
@Tag("L3")
@Import(ProductEditor.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // every repository call commits on its own, like separate HTTP requests
class OptimisticLockingTest extends JpaKataTest {

	@Autowired
	EditableProductRepository products;

	@Autowired
	ProductEditor editor;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void theSecondEditorWithAStaleCopyIsRejected() {
		long id = products.save(new EditableProduct("LMP-9", "Lamp", "living", 4900)).getId();

		EditableProduct formOfA = products.findById(id).orElseThrow(); // detached copies, like two browser forms
		EditableProduct formOfB = products.findById(id).orElseThrow();

		formOfA.rename("Desk Lamp");
		editor.save(formOfA);

		formOfB.rename("Floor Lamp");
		assertThatThrownBy(() -> editor.save(formOfB)).isInstanceOf(ProductEditor.ConcurrentEditException.class);

		assertThat(jdbc.queryForObject("SELECT name FROM jpa_product WHERE id = ?", String.class, id)).isEqualTo("Desk Lamp");
	}

	@Test
	void eachSaveIncrementsTheVersion() {
		long id = products.save(new EditableProduct("RUG-9", "Rug", "living", 8900)).getId();
		EditableProduct p = products.findById(id).orElseThrow();
		p.rename("Big Rug");
		EditableProduct saved = editor.save(p);
		assertThat(saved.getVersion()).isEqualTo(p.getVersion() + 1);
	}

}
