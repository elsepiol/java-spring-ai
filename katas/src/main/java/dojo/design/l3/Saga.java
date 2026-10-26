package dojo.design.l3;

import java.util.List;

import dojo.Kata;

/**
 * KATA design L3 "Saga" (tracks/15-system-design/L3-production.md, section 3).
 *
 * Placing an order touches several services (payment, inventory, shipping), each with its own
 * database: there is no distributed transaction. A saga runs local steps one after another;
 * if one fails, it runs the COMPENSATIONS of the steps that already succeeded, in REVERSE order
 * (refund the payment, release the stock).
 * - run(steps): execute each step's action in order. All succeed -> Outcome.COMPLETED.
 * - A step's action throws -> compensate every previously COMPLETED step in reverse order (not
 *   the failed one: it did nothing, or must clean up itself), then Outcome.COMPENSATED.
 * - A compensation that throws is retried up to {@code compensationAttempts} times in total;
 *   if it still fails, record it and continue with the remaining compensations (stopping would
 *   leave even more inconsistency), then Outcome.NEEDS_ATTENTION: a human or a repair job must
 *   look at it (in production: alert, park in a table).
 * - log(): what happened, in order: "do:<name>", "failed:<name>", "undo:<name>",
 *   "undo-failed:<name>" (one "undo-failed" after the last failed attempt; each attempt is
 *   still an "undo:<name>" entry).
 */
public final class Saga {

	public enum Outcome {
		COMPLETED, COMPENSATED, NEEDS_ATTENTION
	}

	public record Step(String name, Runnable action, Runnable compensation) {
	}

	public Saga(int compensationAttempts) {
		Kata.todo("tracks/15-system-design/L3-production.md section 3");
	}

	public Outcome run(List<Step> steps) {
		return Kata.todo("tracks/15-system-design/L3-production.md section 3");
	}

	public List<String> log() {
		return Kata.todo("tracks/15-system-design/L3-production.md section 3");
	}

}
