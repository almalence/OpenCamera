package com.almalence.util;

public class FpsMeasurer
{
	private final int		width;

	private volatile long	lastTime	= System.currentTimeMillis();
	private volatile float	fps			= 0.0f;
	private volatile long	interval	= 0;

	/**
	 * Creates <code>FpsMeasurer</code> instance without window (width=1).
	 */
	public FpsMeasurer()
	{
		this.width = 1;
	}

	/**
	 * Creates <code>FpsMeasurer</code> instance with specified measurement
	 * window width.
	 * 
	 * @param window_width
	 *            Measurement window width
	 */
	public FpsMeasurer(final int window_width)
	{
		this.width = window_width;
	}

	/**
	 * Measures time since last call and calculates FPS based on the time
	 * interval and window width.
	 * 
	 * @return current FPS;
	 */
	public float measure()
	{
		final long time = System.currentTimeMillis();
		this.interval = time - this.lastTime;
		this.fps += ((1000.0f / Math.max(this.interval, 1)) - this.fps) / this.width;
		this.lastTime = time;

		return this.fps;
	}

	/**
	 * @param interval
	 * @return
	 */
	public float addMeasurement(final long interval)
	{
		if (interval > 0)
		{
			final long time = System.currentTimeMillis();
			this.interval = interval;
			this.fps += ((1000.0f / this.interval) - this.fps) / this.width;
			this.lastTime = time;
		}

		return this.fps;
	}

	/**
	 * Returns currently measured FPS.
	 * 
	 * @return current FPS
	 */
	public float getFPS()
	{
		return this.fps;
	}

	/**
	 * Returns last measurements interval.
	 * 
	 * @return last measurements interval
	 */
	public long getLastInterval()
	{
		return this.interval;
	}

	/**
	 * Clears all the collected data.
	 */
	public void flush()
	{
		this.lastTime = System.currentTimeMillis();
		this.fps = 0.0f;
		this.interval = 0;
	}
}
