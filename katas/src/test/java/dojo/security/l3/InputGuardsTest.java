package dojo.security.l3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("security")
@Tag("L3")
class InputGuardsTest {

	@Nested
	class EscapeHtml {

		@Test
		void escapesTheFiveSpecialCharacters() {
			assertThat(InputGuards.escapeHtml("<script>alert('x')</script>"))
				.isEqualTo("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;");
			assertThat(InputGuards.escapeHtml("\" onmouseover=\"steal()")).isEqualTo("&quot; onmouseover=&quot;steal()");
		}

		@Test
		void escapesAmpersandsExactlyOnce() {
			assertThat(InputGuards.escapeHtml("Tom & Jerry &lt;")).isEqualTo("Tom &amp; Jerry &amp;lt;");
		}

		@Test
		void leavesOrdinaryTextAloneAndHandlesNull() {
			assertThat(InputGuards.escapeHtml("Grüße, 100%")).isEqualTo("Grüße, 100%");
			assertThat(InputGuards.escapeHtml(null)).isEmpty();
		}

	}

	@Nested
	class ResolveUpload {

		@TempDir
		Path base;

		@Test
		void resolvesNamesInsideTheBaseDirectory() {
			assertThat(InputGuards.resolveUpload(base, "avatar.png")).isEqualTo(base.resolve("avatar.png").toAbsolutePath().normalize());
			assertThat(InputGuards.resolveUpload(base, "2026/01/report.pdf").startsWith(base.toAbsolutePath().normalize())).isTrue();
			assertThat(InputGuards.resolveUpload(base, "a/../b.txt")).isEqualTo(base.resolve("b.txt").toAbsolutePath().normalize());
		}

		@Test
		void rejectsTraversal() {
			for (String evil : new String[] { "../secret", "../../etc/passwd", "a/../../x", "./../x" }) {
				assertThatIllegalArgumentException().as(evil).isThrownBy(() -> InputGuards.resolveUpload(base, evil));
			}
		}

		@Test
		void rejectsSiblingDirectoriesThatShareAPrefix() {
			Path uploads = base.resolve("up");
			String sibling = "../up-old/x.txt"; // "/tmp/.../up-old" starts with the STRING "/tmp/.../up"
			assertThatIllegalArgumentException().isThrownBy(() -> InputGuards.resolveUpload(uploads, sibling));
		}

		@Test
		void rejectsAbsolutePathsTheBaseItselfAndNul() {
			assertThatIllegalArgumentException().isThrownBy(() -> InputGuards.resolveUpload(base, "/etc/passwd"));
			assertThatIllegalArgumentException().isThrownBy(() -> InputGuards.resolveUpload(base, "."));
			assertThatIllegalArgumentException().isThrownBy(() -> InputGuards.resolveUpload(base, "a/.."));
			assertThatIllegalArgumentException().isThrownBy(() -> InputGuards.resolveUpload(base, "x.png\0.txt"));
			assertThatIllegalArgumentException().isThrownBy(() -> InputGuards.resolveUpload(base, ""));
		}

	}

	@Nested
	class SafeRedirect {

		private final InputGuards guards = new InputGuards(Set.of("shop.example.com"));

		@Test
		void allowsLocalPaths() {
			assertThat(guards.isSafeRedirect("/orders/42")).isTrue();
			assertThat(guards.isSafeRedirect("/")).isTrue();
		}

		@Test
		void allowsHttpsToAllowedHostsOnly() {
			assertThat(guards.isSafeRedirect("https://shop.example.com/cart")).isTrue();
			assertThat(guards.isSafeRedirect("https://SHOP.example.com/cart")).isTrue();
			assertThat(guards.isSafeRedirect("https://evil.net/")).isFalse();
			assertThat(guards.isSafeRedirect("http://shop.example.com/")).isFalse();
		}

		@Test
		void rejectsTheClassicBypasses() {
			for (String evil : new String[] { "//evil.net", "/\\evil.net", "https://shop.example.com.evil.net/",
					"https://evil.net/?shop.example.com", "https://shop.example.com@evil.net/", "javascript:alert(1)",
					"data:text/html,hi", "evil.net", "/ok\r\nSet-Cookie: x=1", "", null }) {
				assertThat(guards.isSafeRedirect(evil)).as(String.valueOf(evil)).isFalse();
			}
		}

	}

}
