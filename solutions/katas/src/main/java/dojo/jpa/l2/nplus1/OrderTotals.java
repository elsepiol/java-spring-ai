package dojo.jpa.l2.nplus1;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reference solution: one aggregate query instead of 1 + N. */
@Service
public class OrderTotals {

	private final OrderHeaderRepository orders;

	public OrderTotals(OrderHeaderRepository orders) {
		this.orders = orders;
	}

	@Transactional(readOnly = true)
	public Map<Long, Long> totalsPerOrder() {
		Map<Long, Long> totals = new LinkedHashMap<>();
		for (OrderHeaderRepository.OrderTotal t : orders.totals()) {
			totals.put(t.getOrderId(), t.getTotal());
		}
		return totals;
	}

}
