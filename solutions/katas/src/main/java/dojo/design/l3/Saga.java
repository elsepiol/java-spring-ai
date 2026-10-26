package dojo.design.l3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Reference solution for the Saga kata: an orchestrator with a stack of completed steps. */
public final class Saga {

	public enum Outcome {
		COMPLETED, COMPENSATED, NEEDS_ATTENTION
	}

	public record Step(String name, Runnable action, Runnable compensation) {
	}

	private final int compensationAttempts;
	private final List<String> log = new ArrayList<>();

	public Saga(int compensationAttempts) {
		if (compensationAttempts < 1) {
			throw new IllegalArgumentException("at least one attempt");
		}
		this.compensationAttempts = compensationAttempts;
	}

	public Outcome run(List<Step> steps) {
		Deque<Step> completed = new ArrayDeque<>();
		for (Step step : steps) {
			log.add("do:" + step.name());
			try {
				step.action().run();
				completed.push(step);
			}
			catch (RuntimeException e) {
				log.add("failed:" + step.name());
				return compensate(completed);
			}
		}
		return Outcome.COMPLETED;
	}

	public List<String> log() {
		return List.copyOf(log);
	}

	private Outcome compensate(Deque<Step> completed) {
		boolean allUndone = true;
		while (!completed.isEmpty()) {
			Step step = completed.pop(); // most recent first
			boolean undone = false;
			for (int attempt = 1; attempt <= compensationAttempts && !undone; attempt++) {
				log.add("undo:" + step.name());
				try {
					step.compensation().run();
					undone = true;
				}
				catch (RuntimeException e) {
					// retry; compensations must be idempotent for this to be safe
				}
			}
			if (!undone) {
				log.add("undo-failed:" + step.name());
				allUndone = false;
			}
		}
		return allUndone ? Outcome.COMPENSATED : Outcome.NEEDS_ATTENTION;
	}

}
