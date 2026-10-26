package dojo.security.l2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("security")
@Tag("L2")
class JwtCodecTest {

	private static final byte[] SECRET = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

	private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
	private final JwtCodec codec = new JwtCodec(SECRET, clock);

	@Test
	void roundTripsTheClaims() {
		String token = codec.issue("alice", List.of("USER", "ADMIN"), Duration.ofMinutes(15));
		JwtCodec.Claims claims = codec.verify(token);
		assertThat(claims.subject()).isEqualTo("alice");
		assertThat(claims.roles()).containsExactly("USER", "ADMIN");
		assertThat(claims.expiresAt()).isEqualTo(Instant.parse("2026-01-01T10:15:00Z"));
	}

	@Test
	void producesAStandardHs256Token() throws Exception {
		String token = codec.issue("alice", List.of("USER"), Duration.ofMinutes(15));
		assertThat(token).doesNotContain("=", "+", "/");
		String[] parts = token.split("\\.");
		assertThat(parts).hasSize(3);
		assertThat(text(parts[0])).contains("\"alg\":\"HS256\"");
		assertThat(text(parts[1])).contains("\"sub\":\"alice\"", "\"exp\":" + Instant.parse("2026-01-01T10:15:00Z").getEpochSecond());
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(SECRET, "HmacSHA256"));
		byte[] expected = mac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
		assertThat(parts[2]).isEqualTo(Base64.getUrlEncoder().withoutPadding().encodeToString(expected));
	}

	@Test
	void payloadIsOnlyEncodedNotEncrypted() {
		String token = codec.issue("alice", List.of("USER"), Duration.ofMinutes(15));
		assertThat(text(token.split("\\.")[1])).contains("alice"); // anyone can read it: never put secrets in a JWT
	}

	@Test
	void rejectsATamperedPayload() {
		String token = codec.issue("alice", List.of("USER"), Duration.ofMinutes(15));
		String[] parts = token.split("\\.");
		String forged = b64(text(parts[1]).replace("\"USER\"", "\"ADMIN\""));
		assertThatThrownBy(() -> codec.verify(parts[0] + "." + forged + "." + parts[2]))
			.isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void rejectsTheNoneAlgorithm() {
		String header = b64("{\"alg\":\"none\",\"typ\":\"JWT\"}");
		String payload = b64("{\"sub\":\"root\",\"roles\":[\"ADMIN\"],\"exp\":" + Instant.parse("2030-01-01T00:00:00Z").getEpochSecond() + "}");
		assertThatThrownBy(() -> codec.verify(header + "." + payload + ".")).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void rejectsOtherAlgorithmsEvenWithAValidHs256Signature() throws Exception {
		String header = b64("{\"alg\":\"HS512\",\"typ\":\"JWT\"}");
		String payload = b64("{\"sub\":\"alice\",\"exp\":" + Instant.parse("2030-01-01T00:00:00Z").getEpochSecond() + "}");
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(SECRET, "HmacSHA256"));
		String sig = Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal((header + "." + payload).getBytes(StandardCharsets.US_ASCII)));
		assertThatThrownBy(() -> codec.verify(header + "." + payload + "." + sig)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void rejectsTokensSignedWithAnotherSecret() {
		JwtCodec other = new JwtCodec("ffffffffffffffffffffffffffffffff".getBytes(StandardCharsets.US_ASCII), clock);
		String token = other.issue("alice", List.of("USER"), Duration.ofMinutes(15));
		assertThatThrownBy(() -> codec.verify(token)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void honoursExpiryWithASmallClockSkew() {
		String token = codec.issue("alice", List.of("USER"), Duration.ofMinutes(15));
		clock.now = Instant.parse("2026-01-01T10:15:29Z");
		assertThat(codec.verify(token).subject()).isEqualTo("alice");
		clock.now = Instant.parse("2026-01-01T10:15:30Z");
		assertThatThrownBy(() -> codec.verify(token)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void rejectsTokensWithoutExpiryOrSubject() throws Exception {
		assertThatThrownBy(() -> codec.verify(signed("{\"sub\":\"alice\"}"))).isInstanceOf(InvalidTokenException.class);
		long exp = Instant.parse("2030-01-01T00:00:00Z").getEpochSecond();
		assertThatThrownBy(() -> codec.verify(signed("{\"exp\":" + exp + "}"))).isInstanceOf(InvalidTokenException.class);
		assertThatThrownBy(() -> codec.verify(signed("{\"sub\":\"  \",\"exp\":" + exp + "}"))).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void rejectsGarbage() {
		for (String bad : new String[] { null, "", "abc", "a.b", "a.b.c.d", "!!!.???.***", b64("[]") + "." + b64("{}") + ".x" }) {
			assertThatThrownBy(() -> codec.verify(bad)).as(String.valueOf(bad)).isInstanceOf(InvalidTokenException.class);
		}
	}

	@Test
	void refusesWeakSecrets() {
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtCodec("secret".getBytes(StandardCharsets.US_ASCII), clock));
	}

	private static String signed(String payloadJson) throws Exception {
		String header = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
		String payload = b64(payloadJson);
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(SECRET, "HmacSHA256"));
		return header + "." + payload + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal((header + "." + payload).getBytes(StandardCharsets.US_ASCII)));
	}

	private static String b64(String json) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}

	private static String text(String part) {
		return new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8);
	}

	static final class MutableClock extends Clock {

		Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}

	}

}
