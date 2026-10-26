package dojo.ops.l3;

import java.time.Instant;
import java.util.List;

import dojo.Kata;

/**
 * KATA ops L3 "CertificateCheck" (tracks/14-git-linux-networking/L3-production.md, section 3).
 *
 * Two checks every TLS client makes (and the two most common causes of "TLS handshake failed"):
 *
 * hostMatches(host, subjectAltNames): entries are "DNS:name" or "IP:address" (the Common Name is
 * ignored, as modern clients do).
 * - DNS names compare case-insensitively, ignoring one trailing dot ("Shop.Example.com." matches
 *   "DNS:shop.example.com");
 * - a wildcard is only allowed as the ENTIRE leftmost label and matches exactly ONE label:
 *   "*.example.com" matches "api.example.com", not "example.com" and not "a.b.example.com";
 *   partial wildcards ("api*.example.com"), wildcards elsewhere ("api.*.com") and wildcards
 *   directly on a top-level domain ("*.com") match nothing;
 * - an IP address host (dotted IPv4) matches only an equal "IP:" entry, never a DNS or wildcard
 *   entry.
 *
 * validity(notBefore, notAfter, now, warnBefore): NOT_YET_VALID before notBefore, EXPIRED after
 * notAfter (both bounds inclusive: valid AT notAfter), EXPIRING_SOON when fewer than warnBefore
 * remain (alert before the outage, not after), else VALID.
 */
public final class CertificateCheck {

	public enum Validity {
		NOT_YET_VALID, VALID, EXPIRING_SOON, EXPIRED
	}

	private CertificateCheck() {
	}

	public static boolean hostMatches(String host, List<String> subjectAltNames) {
		return Kata.todo("tracks/14-git-linux-networking/L3-production.md section 3");
	}

	public static Validity validity(Instant notBefore, Instant notAfter, Instant now, java.time.Duration warnBefore) {
		return Kata.todo("tracks/14-git-linux-networking/L3-production.md section 3");
	}

}
