package com.eternalerkle.speedrun.vitals;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One number shared by several players, such as health or food level. Holds no Minecraft types so it can be unit tested.
 * <p>
 * Each tick: {@link #observe} every player, {@link #apply} once, then {@link #writeFor} every player. A player's change
 * is measured against the value last written to them, so changes made by vanilla between ticks add up across players.
 */
public final class SharedValue {
	private final Map<UUID, Float> written = new HashMap<>();
	private float value;
	private boolean initialized;
	private float pending;

	public boolean isInitialized() {
		return initialized;
	}

	public float value() {
		return value;
	}

	public void set(float value) {
		this.value = value;
		this.initialized = true;
	}

	public boolean isTracked(UUID player) {
		return written.containsKey(player);
	}

	/**
	 * Adds how far a player's value moved since it was last written to them, and returns that change.
	 * {@code cap} is the player's current maximum: if it dropped below the written value, the forced clamp is not
	 * counted as a change. Players never written to contribute nothing.
	 */
	public float observe(UUID player, float current, float cap) {
		Float last = written.get(player);
		if (last == null || !Float.isFinite(current)) {
			// A NaN would poison the shared value for every player, permanently.
			return 0;
		}
		float delta = current - Math.min(last, cap);
		pending += delta;
		return delta;
	}

	/** Applies every observed change to the shared value, clamps it, and returns it. */
	public float apply(float min, float max) {
		float next = value + pending;
		value = Float.isFinite(next) ? clamp(next, min, max) : clamp(value, min, max);
		pending = 0;
		return value;
	}

	/** The value a player with the given personal maximum should hold. Remembered as written to them. */
	public float writeFor(UUID player, float cap) {
		float result = Math.min(value, cap);
		written.put(player, result);
		return result;
	}

	public void forget(UUID player) {
		written.remove(player);
	}

	public void clear() {
		written.clear();
		value = 0;
		pending = 0;
		initialized = false;
	}

	static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}
