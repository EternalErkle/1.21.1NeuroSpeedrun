package com.eternalerkle.speedrun.room;

/** Pure bounce math for the death room face. Coordinates are the face center, relative to the screen center, in blocks. */
public final class DvdBounce {
	public record Step(boolean hitX, boolean hitY) {
		public boolean corner() {
			return hitX && hitY;
		}
	}

	private final double maxX;
	private final double maxY;
	public double x;
	public double y;
	private double vx;
	private double vy;

	/**
	 * @param halfWidth  half the visible plane width
	 * @param halfHeight half the visible plane height
	 * @param size       face edge length
	 * @param speed      speed along each axis in blocks per second
	 */
	public DvdBounce(double halfWidth, double halfHeight, double size, double speed, double startX, double startY, boolean right, boolean up) {
		this.maxX = Math.max(0, halfWidth - size / 2);
		this.maxY = Math.max(0, halfHeight - size / 2);
		this.x = clamp(startX, maxX);
		this.y = clamp(startY, maxY);
		this.vx = right ? speed : -speed;
		this.vy = up ? speed : -speed;
	}

	public Step step(double seconds) {
		x += vx * seconds;
		y += vy * seconds;
		boolean hitX = false;
		boolean hitY = false;
		if (x > maxX) {
			x = 2 * maxX - x;
			vx = -Math.abs(vx);
			hitX = true;
		} else if (x < -maxX) {
			x = -2 * maxX - x;
			vx = Math.abs(vx);
			hitX = true;
		}
		if (y > maxY) {
			y = 2 * maxY - y;
			vy = -Math.abs(vy);
			hitY = true;
		} else if (y < -maxY) {
			y = -2 * maxY - y;
			vy = Math.abs(vy);
			hitY = true;
		}
		x = clamp(x, maxX);
		y = clamp(y, maxY);
		return new Step(hitX, hitY);
	}

	private static double clamp(double value, double max) {
		return Math.max(-max, Math.min(max, value));
	}
}
