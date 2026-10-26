package dojo.spring.l2;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Reference solution. A prototype injected into a singleton is created ONCE, when the singleton is
 * created: the singleton keeps that one instance forever. Ask the container for a new one each time
 * instead: ObjectProvider (or Provider<T>, @Lookup, or a scoped proxy).
 */
@Component
public class ReportService {

	@Component
	@Scope("prototype")
	public static class ReportBuilder {
		private final List<String> lines = new ArrayList<>();

		public ReportBuilder add(String line) {
			lines.add(line);
			return this;
		}

		public String build() {
			return String.join("\n", lines);
		}
	}

	private final ObjectProvider<ReportBuilder> builders;

	public ReportService(ObjectProvider<ReportBuilder> builders) {
		this.builders = builders;
	}

	public String dailyReport(String day, List<String> orders) {
		ReportBuilder builder = builders.getObject(); // a new prototype instance per call
		builder.add("Report " + day);
		orders.forEach(o -> builder.add("- " + o));
		return builder.build();
	}

}
