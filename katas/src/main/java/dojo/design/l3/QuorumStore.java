package dojo.design.l3;

import java.util.Optional;

import dojo.Kata;

/**
 * KATA design L3 "QuorumStore" (tracks/15-system-design/L3-production.md, section 2).
 *
 * A leaderless replicated key-value store (Dynamo, Cassandra) with N replicas, write quorum W and
 * read quorum R. Replicas are numbered 0..N-1 and can be taken down and up (setUp).
 * - put(key, value): the coordinator assigns version = (highest version it can see among the UP
 *   replicas for that key) + 1 and writes (value, version) to every UP replica; it succeeds if at
 *   least W replicas acknowledged, else throws QuorumException (the replicas that did store it
 *   keep it: a failed write is not rolled back, exactly as in the real systems);
 * - get(key): asks the UP replicas; fewer than R up -> QuorumException; otherwise returns the value
 *   with the highest version among the first R up replicas IN INDEX ORDER (the ones that "answered
 *   first"), and READ REPAIR: writes that newest version to those R replicas that had an older
 *   one or none;
 * - with R + W > N every read quorum overlaps every write quorum, so reads see the latest
 *   successful write; with R + W <= N stale reads are possible (the tests show both).
 */
public final class QuorumStore {

	public static final class QuorumException extends RuntimeException {

		public QuorumException(String message) {
			super(message);
		}

	}

	public QuorumStore(int n, int w, int r) {
		Kata.todo("tracks/15-system-design/L3-production.md section 2");
	}

	public void setUp(int replica, boolean up) {
		Kata.todo("tracks/15-system-design/L3-production.md section 2");
	}

	public void put(String key, String value) {
		Kata.todo("tracks/15-system-design/L3-production.md section 2");
	}

	public Optional<String> get(String key) {
		return Kata.todo("tracks/15-system-design/L3-production.md section 2");
	}

	/** For tests: the version replica i holds for key, 0 if none. */
	public long versionAt(int replica, String key) {
		return Kata.todo("tracks/15-system-design/L3-production.md section 2");
	}

}
