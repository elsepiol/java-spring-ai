package dojo.docker.l2;

import java.time.Clock;
import java.time.Duration;

import dojo.Kata;

/**
 * KATA docker L2 "AppHealth" (tracks/11-docker-deploy/L2-practical.md, section 3).
 *
 * The three Kubernetes probes answer three different questions; mixing them up causes outages.
 * - startup: has the application finished starting (startupComplete() called)? Until then
 *   Kubernetes does not run the other probes.
 * - liveness: is this process broken beyond self-repair, so that a RESTART helps? Here: the main
 *   worker loop has not sent a heartbeat for longer than {@code stuckAfter} (a deadlock). A
 *   database outage or a shutdown in progress must NOT fail liveness: restarting every pod
 *   because the database is down only adds a restart storm to the outage.
 * - readiness: should this pod get traffic NOW? DOWN before startup completes, while any
 *   dependency is down, and from the moment shutdown starts (so the load balancer stops sending
 *   requests before the process exits).
 * The heartbeat counts as fresh at construction time. Use the injected clock.
 */
public final class AppHealth {

	public AppHealth(Clock clock, Duration stuckAfter) {
		Kata.todo("tracks/11-docker-deploy/L2-practical.md section 3");
	}

	public void startupComplete() {
		Kata.todo("tracks/11-docker-deploy/L2-practical.md section 3");
	}

	public void heartbeat() {
		Kata.todo("tracks/11-docker-deploy/L2-practical.md section 3");
	}

	public void dependencyDown(String name) {
		Kata.todo("tracks/11-docker-deploy/L2-practical.md section 3");
	}

	public void dependencyUp(String name) {
		Kata.todo("tracks/11-docker-deploy/L2-practical.md section 3");
	}

	public void shutdownStarted() {
		Kata.todo("tracks/11-docker-deploy/L2-practical.md section 3");
	}

	public Status startup() {
		return Kata.todo("tracks/11-docker-deploy/L2-practical.md section 3");
	}

	public Status liveness() {
		return Kata.todo("tracks/11-docker-deploy/L2-practical.md section 3");
	}

	public Status readiness() {
		return Kata.todo("tracks/11-docker-deploy/L2-practical.md section 3");
	}

}
