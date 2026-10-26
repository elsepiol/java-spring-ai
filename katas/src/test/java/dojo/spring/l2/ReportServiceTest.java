package dojo.spring.l2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@Tag("spring")
@Tag("L2")
class ReportServiceTest {

	@Test
	void eachReportStartsEmpty() {
		try (var context = new AnnotationConfigApplicationContext(ReportService.class, ReportService.ReportBuilder.class)) {
			ReportService reports = context.getBean(ReportService.class);
			assertThat(reports.dailyReport("Mon", List.of("#1", "#2"))).isEqualTo("Report Mon\n- #1\n- #2");
			assertThat(reports.dailyReport("Tue", List.of("#3"))).isEqualTo("Report Tue\n- #3");
		}
	}

	@Test
	void theServiceIsStillASingletonAndTheBuilderAPrototype() {
		try (var context = new AnnotationConfigApplicationContext(ReportService.class, ReportService.ReportBuilder.class)) {
			assertThat(context.getBean(ReportService.class)).isSameAs(context.getBean(ReportService.class));
			assertThat(context.getBean(ReportService.ReportBuilder.class)).isNotSameAs(context.getBean(ReportService.ReportBuilder.class));
		}
	}

}
