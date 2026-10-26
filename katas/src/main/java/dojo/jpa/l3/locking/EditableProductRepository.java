package dojo.jpa.l3.locking;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EditableProductRepository extends JpaRepository<EditableProduct, Long> {
}
