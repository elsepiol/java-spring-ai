package dojo.ops.l3;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dojo.ops.l3.CertificateCheck.Validity;

@Tag("ops")
@Tag("L3")
class CertificateCheckTest {

	private static final List<String> SANS = List.of("DNS:shop.example.com", "DNS:*.api.example.com", "IP:10.0.0.7");

	@Test
	void matchesExactNamesCaseInsensitively() {
		assertThat(CertificateCheck.hostMatches("shop.example.com", SANS)).isTrue();
		assertThat(CertificateCheck.hostMatches("SHOP.Example.COM", SANS)).isTrue();
		assertThat(CertificateCheck.hostMatches("shop.example.com.", SANS)).isTrue();
		assertThat(CertificateCheck.hostMatches("www.example.com", SANS)).isFalse();
		assertThat(CertificateCheck.hostMatches("shop.example.co", SANS)).isFalse();
	}

	@Test
	void wildcardsCoverExactlyOneLabel() {
		assertThat(CertificateCheck.hostMatches("eu.api.example.com", SANS)).isTrue();
		assertThat(CertificateCheck.hostMatches("api.example.com", SANS)).isFalse();
		assertThat(CertificateCheck.hostMatches("a.eu.api.example.com", SANS)).isFalse();
		assertThat(CertificateCheck.hostMatches("evilapi.example.com", SANS)).isFalse();
	}

	@Test
	void rejectsDangerousWildcards() {
		assertThat(CertificateCheck.hostMatches("api1.example.com", List.of("DNS:api*.example.com"))).isFalse();
		assertThat(CertificateCheck.hostMatches("api.shop.com", List.of("DNS:api.*.com"))).isFalse();
		assertThat(CertificateCheck.hostMatches("example.com", List.of("DNS:*.com"))).isFalse();
		assertThat(CertificateCheck.hostMatches("anything.example", List.of("DNS:*"))).isFalse();
	}

	@Test
	void ipAddressesNeedIpEntries() {
		assertThat(CertificateCheck.hostMatches("10.0.0.7", SANS)).isTrue();
		assertThat(CertificateCheck.hostMatches("10.0.0.8", SANS)).isFalse();
		assertThat(CertificateCheck.hostMatches("10.0.0.7", List.of("DNS:10.0.0.7"))).isFalse();
		assertThat(CertificateCheck.hostMatches("1.2.3.4", List.of("DNS:*.2.3.4"))).isFalse();
		assertThat(CertificateCheck.hostMatches("shop.example.com", List.of("IP:shop.example.com"))).isFalse();
	}

	@Test
	void checksTheValidityWindow() {
		Instant from = Instant.parse("2026-01-01T00:00:00Z");
		Instant to = Instant.parse("2026-04-01T00:00:00Z");
		Duration warn = Duration.ofDays(14);
		assertThat(CertificateCheck.validity(from, to, Instant.parse("2025-12-31T23:59:59Z"), warn)).isEqualTo(Validity.NOT_YET_VALID);
		assertThat(CertificateCheck.validity(from, to, from, warn)).isEqualTo(Validity.VALID);
		assertThat(CertificateCheck.validity(from, to, Instant.parse("2026-03-18T00:00:00Z"), warn)).isEqualTo(Validity.VALID);
		assertThat(CertificateCheck.validity(from, to, Instant.parse("2026-03-18T00:00:01Z"), warn)).isEqualTo(Validity.EXPIRING_SOON);
		assertThat(CertificateCheck.validity(from, to, to, warn)).isEqualTo(Validity.EXPIRING_SOON);
		assertThat(CertificateCheck.validity(from, to, to.plusSeconds(1), warn)).isEqualTo(Validity.EXPIRED);
	}

}
