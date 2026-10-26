package dojo.security.l3;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Reference solution for the LoginThrottle kata. */
public final class LoginThrottle {

	private static final class Account {

		final Deque<Instant> failures = new ArrayDeque<>();
		Instant lockedUntil = Instant.MIN;

	}

	private final Clock clock;
	private final int maxFailures;
	private final Duration window;
	private final Duration lockout;
	private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();

	public LoginThrottle(Clock clock, int maxFailures, Duration window, Duration lockout) {
		this.clock = clock;
		this.maxFailures = maxFailures;
		this.window = window;
		this.lockout = lockout;
	}

	public boolean isLocked(String username) {
		return retryAfter(username).isPositive();
	}

	public Duration retryAfter(String username) {
		Account account = accounts.get(key(username));
		if (account == null) {
			return Duration.ZERO;
		}
		synchronized (account) {
			Instant now = clock.instant();
			return now.isBefore(account.lockedUntil) ? Duration.between(now, account.lockedUntil) : Duration.ZERO;
		}
	}

	public void recordFailure(String username) {
		Account account = accounts.computeIfAbsent(key(username), k -> new Account());
		synchronized (account) {
			Instant now = clock.instant();
			if (now.isBefore(account.lockedUntil)) {
				return;
			}
			while (!account.failures.isEmpty() && !account.failures.peekFirst().isAfter(now.minus(window))) {
				account.failures.removeFirst();
			}
			account.failures.addLast(now);
			if (account.failures.size() >= maxFailures) {
				account.lockedUntil = now.plus(lockout);
				account.failures.clear();
			}
		}
	}

	public void recordSuccess(String username) {
		Account account = accounts.get(key(username));
		if (account != null) {
			synchronized (account) {
				if (!clock.instant().isBefore(account.lockedUntil)) {
					account.failures.clear();
				}
			}
		}
	}

	private static String key(String username) {
		return username.trim().toLowerCase(Locale.ROOT);
	}

}
