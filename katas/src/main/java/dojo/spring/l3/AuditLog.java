package dojo.spring.l3;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Given. */
public class AuditLog {
	public final List<String> entries = new CopyOnWriteArrayList<>();
}
