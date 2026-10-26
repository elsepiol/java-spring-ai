package dojo.docker.l1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("docker")
@Tag("L1")
class ImageRefTest {

	private static final String DIGEST = "sha256:4f6c0b1d2e3a4f5b6c7d8e9f0a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d";

	@ParameterizedTest(name = "{0}")
	@CsvSource(nullValues = "-", value = {
			"postgres,                          docker.io,       library/postgres, latest",
			"postgres:17,                       docker.io,       library/postgres, 17",
			"eclipse-temurin:21-jre-alpine,     docker.io,       library/eclipse-temurin, 21-jre-alpine",
			"acme/shop:1.4.2,                   docker.io,       acme/shop,        1.4.2",
			"ghcr.io/acme/shop:1.4.2,           ghcr.io,         acme/shop,        1.4.2",
			"localhost:5000/shop,               localhost:5000,  shop,             latest",
			"localhost/shop:dev,                localhost,       shop,             dev",
			"registry.example.com:8443/team/sub/app:2, registry.example.com:8443, team/sub/app, 2" })
	void parsesRegistryRepositoryAndTag(String ref, String registry, String repository, String tag) {
		ImageRef image = ImageRef.parse(ref);
		assertThat(image.registry()).isEqualTo(registry);
		assertThat(image.repository()).isEqualTo(repository);
		assertThat(image.tag()).isEqualTo(tag);
		assertThat(image.digest()).isNull();
		assertThat(image.pinned()).isFalse();
	}

	@Test
	void parsesDigests() {
		ImageRef byDigest = ImageRef.parse("ghcr.io/acme/shop@" + DIGEST);
		assertThat(byDigest.tag()).as("no implicit latest with a digest").isNull();
		assertThat(byDigest.digest()).isEqualTo(DIGEST);
		assertThat(byDigest.pinned()).isTrue();

		ImageRef both = ImageRef.parse("postgres:17@" + DIGEST);
		assertThat(both.repository()).isEqualTo("library/postgres");
		assertThat(both.tag()).isEqualTo("17");
		assertThat(both.digest()).isEqualTo(DIGEST);
	}

	@Test
	void theRegistryPortIsNotATag() {
		ImageRef image = ImageRef.parse("localhost:5000/team/shop");
		assertThat(image.registry()).isEqualTo("localhost:5000");
		assertThat(image.tag()).isEqualTo("latest");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "  ", "Acme/Shop", "shop:", "ghcr.io/", "shop@md5:abc", "shop@sha256:", "a//b" })
	void rejectsInvalidReferences(String ref) {
		assertThatIllegalArgumentException().isThrownBy(() -> ImageRef.parse(ref));
	}

}
