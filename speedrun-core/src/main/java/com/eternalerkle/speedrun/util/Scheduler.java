package com.eternalerkle.speedrun.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs tasks after a delay measured in real seconds, so timing is independent of the tick rate.
 * Polled once per server tick on the server thread.
 */
public final class Scheduler {
	private record Task(long dueNanos, Runnable action) {
	}

	private final List<Task> tasks = new ArrayList<>();

	public void after(double seconds, Runnable action) {
		tasks.add(new Task(System.nanoTime() + (long) (seconds * 1_000_000_000L), action));
	}

	public void tick() {
		if (tasks.isEmpty()) {
			return;
		}
		long now = System.nanoTime();
		List<Task> due = new ArrayList<>();
		tasks.removeIf(task -> {
			if (task.dueNanos <= now) {
				due.add(task);
				return true;
			}
			return false;
		});
		due.forEach(task -> task.action.run());
	}

	public void clear() {
		tasks.clear();
	}
}
