package dojo.spring.l3;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/** Given: the slow remote pricing service. Counts how often it is called. */
@Component
public class PriceSource {
	public final AtomicInteger calls = new AtomicInteger();

	public long fetch(String sku) {
		calls.incrementAndGet();
		return sku.length() * 1000L;
	}
}
