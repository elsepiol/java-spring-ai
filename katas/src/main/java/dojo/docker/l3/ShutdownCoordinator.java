package dojo.docker.l3;

import java.time.Duration;

import dojo.Kata;

/**
 * KATA docker L3 "ShutdownCoordinator" (tracks/11-docker-deploy/L3-production.md, section 2).
 *
 * Graceful shutdown, as Spring Boot's server.shutdown=graceful does it. Request threads call
 * tryBegin() before handling a request and end() when done (in a finally). When the container
 * receives SIGTERM, a shutdown hook calls shutdown():
 * - from then on tryBegin() returns false (the caller answers 503 and the client retries
 *   another pod), and isAcceptingRequests() is false;
 * - shutdown() then WAITS until every in-flight request has called end(), and returns true;
 * - but never longer than {@code drainTimeout} (Kubernetes sends SIGKILL after
 *   terminationGracePeriodSeconds anyway): then it returns false with requests still running;
 * - calling shutdown() a second time just waits again (do not throw).
 * Many threads call these methods at once. No busy waiting: block on a monitor or a Condition.
 */
public final class ShutdownCoordinator {

	public ShutdownCoordinator(Duration drainTimeout) {
		Kata.todo("tracks/11-docker-deploy/L3-production.md section 2");
	}

	public boolean tryBegin() {
		return Kata.todo("tracks/11-docker-deploy/L3-production.md section 2");
	}

	public void end() {
		Kata.todo("tracks/11-docker-deploy/L3-production.md section 2");
	}

	public boolean isAcceptingRequests() {
		return Kata.todo("tracks/11-docker-deploy/L3-production.md section 2");
	}

	public int inFlight() {
		return Kata.todo("tracks/11-docker-deploy/L3-production.md section 2");
	}

	public boolean shutdown() throws InterruptedException {
		return Kata.todo("tracks/11-docker-deploy/L3-production.md section 2");
	}

}
