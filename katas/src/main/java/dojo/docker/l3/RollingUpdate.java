package dojo.docker.l3;

import java.util.List;

import dojo.Kata;

/**
 * KATA docker L3 "RollingUpdate" (tracks/11-docker-deploy/L3-production.md, section 3).
 *
 * Simulate a Kubernetes rolling update from version 1 to version 2 of a Deployment. All pods
 * start as old pods; new pods become ready as soon as they are created. Repeat steps until every
 * pod is new; each step:
 *   1. create new pods, as many as allowed: never more than {@code replicas} new pods in total,
 *      and never more than replicas + maxSurge pods (old + new) at once;
 *   2. then delete old pods, as many as allowed while keeping at least replicas - maxUnavailable
 *      pods (old + new) running;
 * and record the State after the step. maxSurge and maxUnavailable must not be negative and not
 * both 0 (no progress would be possible): IllegalArgumentException.
 *
 * resolve turns Kubernetes' percentages into pod counts: "25%" of 10 replicas is 2.5, which rounds
 * UP for maxSurge (3) and DOWN for maxUnavailable (2). Plain numbers ("1") are absolute.
 */
public final class RollingUpdate {

	public record State(int oldPods, int newPods) {
	}

	private RollingUpdate() {
	}

	public static List<State> plan(int replicas, int maxSurge, int maxUnavailable) {
		return Kata.todo("tracks/11-docker-deploy/L3-production.md section 3");
	}

	/** @return {maxSurge, maxUnavailable} as pod counts */
	public static int[] resolve(int replicas, String maxSurge, String maxUnavailable) {
		return Kata.todo("tracks/11-docker-deploy/L3-production.md section 3");
	}

}
