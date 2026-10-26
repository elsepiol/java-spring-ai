package dojo.jpa.l2.nplus1;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KATA jpa L2 "OrderTotals" (find the bug) (tracks/08-jpa-transactions/L2-practical.md, section 1: N+1).
 *
 * The admin page "order totals" takes 4 seconds in production with 2,000 orders, and the database
 * team asks why this endpoint sends 2,001 queries per call. The test counts the statements for 30
 * orders: make it at most 2, without changing the result. Hints: JOIN FETCH, @EntityGraph, or a
 * projection that lets the database do the sum.
 */
@Service
public class OrderTotals {

	private final OrderHeaderRepository orders;

	public OrderTotals(OrderHeaderRepository orders) {
		this.orders = orders;
	}

	/** order id -> total in cents, in id order. */
	@Transactional(readOnly = true)
	public Map<Long, Long> totalsPerOrder() {
		Map<Long, Long> totals = new LinkedHashMap<>();
		for (OrderHeader order : orders.findAll(org.springframework.data.domain.Sort.by("id"))) {
			long total = 0;
			for (OrderItem item : order.getItems()) { // touching the lazy collection: one query PER ORDER
				total += item.totalCents();
			}
			totals.put(order.getId(), total);
		}
		return totals;
	}

}
