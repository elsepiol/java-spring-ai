package dojo.spring.l2;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * KATA spring L2 "ReportService" (find the bug) (tracks/06-spring-core/L2-practical.md, section 1: scopes).
 *
 * ReportBuilder collects lines for ONE report, so it is a prototype: a new instance every time it
 * is requested. ReportService is a singleton and builds a fresh report per call. Yet reports
 * contain the lines of every previous report, and under load two requests mix their lines.
 * Fix ReportService so each call gets a new ReportBuilder, still from the Spring container (the
 * builder may get dependencies injected later). Do not change ReportBuilder.
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

	private final ReportBuilder builder;

	public ReportService(ReportBuilder builder) {
		this.builder = builder;
	}

	public String dailyReport(String day, List<String> orders) {
		builder.add("Report " + day);
		orders.forEach(o -> builder.add("- " + o));
		return builder.build();
	}

}
