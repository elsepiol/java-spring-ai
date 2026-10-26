package dojo.jpa.l2.nplus1;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Reference solution: two options, both one query. */
public interface OrderHeaderRepository extends JpaRepository<OrderHeader, Long> {

	/** Option 1: load orders WITH their items in one query (distinct: one entity per order despite the join). */
	@Query("select distinct o from OrderHeader o left join fetch o.items order by o.id")
	List<OrderHeader> findAllWithItems();

	/** Option 2 (better here): let the database sum; no entities at all. */
	interface OrderTotal {
		Long getOrderId();

		Long getTotal();
	}

	@Query("select o.id as orderId, coalesce(sum(i.quantity * i.unitPriceCents), 0) as total "
			+ "from OrderHeader o left join o.items i group by o.id order by o.id")
	List<OrderTotal> totals();

}
