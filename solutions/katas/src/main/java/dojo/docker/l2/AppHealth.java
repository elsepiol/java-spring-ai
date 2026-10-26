package dojo.docker.l2;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Reference solution for the AppHealth kata. */
public final class AppHealth {

	private final Clock clock;
	private final Duration stuckAfter;
	private final Set<String> downDependencies = ConcurrentHashMap.newKeySet();
	private volatile Instant lastHeartbeat;
	private volatile boolean started;
	private volatile boolean shuttingDown;

	public AppHealth(Clock clock, Duration stuckAfter) {
		this.clock = clock;
		this.stuckAfter = stuckAfter;
		this.lastHeartbeat = clock.instant();
	}

	public void startupComplete() {
		started = true;
	}

	public void heartbeat() {
		lastHeartbeat = clock.instant();
	}

	public void dependencyDown(String name) {
		downDependencies.add(name);
	}

	public void dependencyUp(String name) {
		downDependencies.remove(name);
	}

	public void shutdownStarted() {
		shuttingDown = true;
	}

	public Status startup() {
		return started ? Status.UP : Status.DOWN;
	}

	public Status liveness() {
		boolean stuck = Duration.between(lastHeartbeat, clock.instant()).compareTo(stuckAfter) > 0;
		return stuck ? Status.DOWN : Status.UP;
	}

	public Status readiness() {
		return started && !shuttingDown && downDependencies.isEmpty() ? Status.UP : Status.DOWN;
	}

}
