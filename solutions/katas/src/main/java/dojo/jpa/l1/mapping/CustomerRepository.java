package dojo.jpa.l1.mapping;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Given. Spring Data generates the implementation, including findByEmail from the method name. */
public interface CustomerRepository extends JpaRepository<Customer, Long> {
	Optional<Customer> findByEmail(String email);
}
