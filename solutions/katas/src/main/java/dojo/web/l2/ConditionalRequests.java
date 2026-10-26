package dojo.web.l2;

import java.util.Arrays;
import java.util.Optional;

/** Reference solution. */
public final class ConditionalRequests {

	public enum Decision { PROCEED, NOT_MODIFIED_304, PRECONDITION_FAILED_412 }

	private ConditionalRequests() {
	}

	public static Decision evaluate(String method, Optional<String> ifMatch, Optional<String> ifNoneMatch, Optional<String> currentEtag) {
		if (ifMatch.isPresent()) {
			boolean ok = currentEtag.isPresent() && (ifMatch.get().strip().equals("*")
					|| tags(ifMatch.get()).anyMatch(t -> !isWeak(t) && !isWeak(currentEtag.get()) && t.equals(currentEtag.get())));
			if (!ok) {
				return Decision.PRECONDITION_FAILED_412;
			}
		} else if (ifNoneMatch.isPresent()) {
			boolean matches = currentEtag.isPresent() && (ifNoneMatch.get().strip().equals("*")
					|| tags(ifNoneMatch.get()).anyMatch(t -> opaque(t).equals(opaque(currentEtag.get()))));
			if (matches) {
				return method.equals("GET") || method.equals("HEAD") ? Decision.NOT_MODIFIED_304 : Decision.PRECONDITION_FAILED_412;
			}
		}
		return Decision.PROCEED;
	}

	private static java.util.stream.Stream<String> tags(String header) {
		return Arrays.stream(header.split(",")).map(String::strip).filter(s -> !s.isEmpty());
	}

	private static boolean isWeak(String tag) {
		return tag.startsWith("W/");
	}

	private static String opaque(String tag) {
		return isWeak(tag) ? tag.substring(2) : tag;
	}

}
