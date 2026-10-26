package dojo.security.l3;

import java.time.Clock;
import java.time.Duration;

import dojo.Kata;

/**
 * KATA security L3 "LoginThrottle" (tracks/09-security/L3-production.md, section 3).
 *
 * Slow down password guessing for one account:
 * - a failure counts for {@code window}; failures older than that are forgotten;
 * - {@code maxFailures} failures inside the window lock the account for {@code lockout}, measured
 *   from the failure that triggered it; while locked, retryAfter() says how long is left
 *   (Duration.ZERO when not locked);
 * - a successful login clears the failures (it is only recorded when not locked);
 * - user names are compared case-insensitively and trimmed: "Alice " and "alice" are one account,
 *   or an attacker gets maxFailures guesses per spelling;
 * - many request threads call this at once: it must be thread-safe.
 * Keep it bounded per user (do not store more than maxFailures timestamps).
 */
public final class LoginThrottle {

	public LoginThrottle(Clock clock, int maxFailures, Duration window, Duration lockout) {
		Kata.todo("tracks/09-security/L3-production.md section 3");
	}

	public boolean isLocked(String username) {
		return Kata.todo("tracks/09-security/L3-production.md section 3");
	}

	public Duration retryAfter(String username) {
		return Kata.todo("tracks/09-security/L3-production.md section 3");
	}

	public void recordFailure(String username) {
		Kata.todo("tracks/09-security/L3-production.md section 3");
	}

	public void recordSuccess(String username) {
		Kata.todo("tracks/09-security/L3-production.md section 3");
	}

}
