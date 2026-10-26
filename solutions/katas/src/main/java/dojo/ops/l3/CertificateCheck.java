package dojo.ops.l3;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Reference solution for the CertificateCheck kata. */
public final class CertificateCheck {

	public enum Validity {
		NOT_YET_VALID, VALID, EXPIRING_SOON, EXPIRED
	}

	private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");

	private CertificateCheck() {
	}

	public static boolean hostMatches(String host, List<String> subjectAltNames) {
		String h = normalise(host);
		boolean isIp = IPV4.matcher(h).matches();
		for (String san : subjectAltNames) {
			if (san.startsWith("IP:")) {
				if (isIp && san.substring(3).equals(h)) {
					return true;
				}
			}
			else if (san.startsWith("DNS:") && !isIp && dnsMatches(h, normalise(san.substring(4)))) {
				return true;
			}
		}
		return false;
	}

	public static Validity validity(Instant notBefore, Instant notAfter, Instant now, Duration warnBefore) {
		if (now.isBefore(notBefore)) {
			return Validity.NOT_YET_VALID;
		}
		if (now.isAfter(notAfter)) {
			return Validity.EXPIRED;
		}
		return Duration.between(now, notAfter).compareTo(warnBefore) < 0 ? Validity.EXPIRING_SOON : Validity.VALID;
	}

	private static boolean dnsMatches(String host, String pattern) {
		if (!pattern.contains("*")) {
			return host.equals(pattern);
		}
		if (!pattern.startsWith("*.") || pattern.indexOf('*', 1) >= 0) {
			return false;
		}
		String suffix = pattern.substring(2);
		if (!suffix.contains(".")) {
			return false; // *.com
		}
		int dot = host.indexOf('.');
		return dot > 0 && host.substring(dot + 1).equals(suffix);
	}

	private static String normalise(String name) {
		String n = name.toLowerCase(Locale.ROOT);
		return n.endsWith(".") ? n.substring(0, n.length() - 1) : n;
	}

}
