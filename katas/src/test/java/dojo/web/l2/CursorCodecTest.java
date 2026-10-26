package dojo.web.l2;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dojo.web.l2.CursorCodec.InvalidCursorException;
import dojo.web.l2.CursorCodec.Position;
import java.util.Base64;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("web")
@Tag("L2")
class CursorCodecTest {

	private final CursorCodec codec = new CursorCodec("server-secret-key-of-at-least-32-bytes!".getBytes(UTF_8));

	@Test
	void roundTrips() {
		Position p = new Position(4900, 4);
		assertThat(codec.decode(codec.encode(p))).isEqualTo(p);
	}

	@Test
	void isUrlSafeAndOpaque() {
		String cursor = codec.encode(new Position(123456789, 987654321));
		assertThat(cursor).matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+").doesNotContain("123456789");
	}

	@Test
	void aTamperedCursorIsRejected() {
		String cursor = codec.encode(new Position(4900, 4));
		String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString("0:0".getBytes(UTF_8));
		String forged = forgedPayload + cursor.substring(cursor.indexOf('.'));
		assertThatThrownBy(() -> codec.decode(forged)).isInstanceOf(InvalidCursorException.class);
	}

	@Test
	void aCursorFromAnotherServerKeyIsRejected() {
		CursorCodec other = new CursorCodec("a-different-secret-key-for-staging!!".getBytes(UTF_8));
		String cursor = other.encode(new Position(1, 1));
		assertThatThrownBy(() -> codec.decode(cursor)).isInstanceOf(InvalidCursorException.class);
	}

	@Test
	void garbageIsRejected() {
		for (String bad : new String[] { "", "abc", "abc.", ".abc", "!!!.???", "YWJj.YWJj" }) {
			assertThatThrownBy(() -> codec.decode(bad)).as(bad).isInstanceOf(InvalidCursorException.class);
		}
	}

}
