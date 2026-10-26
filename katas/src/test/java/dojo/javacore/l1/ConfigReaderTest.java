package dojo.javacore.l1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dojo.javacore.l1.ConfigReader.ConfigException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("javacore")
@Tag("L1")
class ConfigReaderTest {

	@TempDir
	Path dir;

	private Path config(String content) throws IOException {
		return Files.writeString(dir.resolve("app.conf"), content);
	}

	@Test
	void readsThePort() throws Exception {
		assertThat(ConfigReader.readPort(config("# web server\n\nhost=localhost\n  port = 8080  \n"))).isEqualTo(8080);
	}

	@Test
	void aMissingFileIsAConfigExceptionThatKeepsTheRealCause() {
		Path missing = dir.resolve("nope.conf");
		assertThatThrownBy(() -> ConfigReader.readPort(missing))
				.isInstanceOf(ConfigException.class)
				.hasMessageContaining("nope.conf")
				.hasCauseInstanceOf(NoSuchFileException.class);
	}

	@Test
	void aMissingPortIsReported() throws Exception {
		Path file = config("host=localhost\n#port=1\n");
		assertThatThrownBy(() -> ConfigReader.readPort(file)).isInstanceOf(ConfigException.class).hasMessageContaining("port is missing");
	}

	@Test
	void aPortThatIsNotANumberIsReportedWithTheValue() throws Exception {
		Path file = config("port=abc\n");
		assertThatThrownBy(() -> ConfigReader.readPort(file))
				.isInstanceOf(ConfigException.class)
				.hasMessageContaining("abc")
				.hasCauseInstanceOf(NumberFormatException.class);
	}

	@Test
	void aPortOutOfRangeIsReported() throws Exception {
		Path file = config("port=70000\n");
		assertThatThrownBy(() -> ConfigReader.readPort(file)).isInstanceOf(ConfigException.class).hasMessageContaining("1..65535");
	}

	@Test
	void whenPortAppearsTwiceTheLastOneWins() throws Exception {
		assertThat(ConfigReader.readPort(config("port=8080\nport=9090\n"))).isEqualTo(9090);
	}

}
