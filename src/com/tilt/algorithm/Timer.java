package com.tilt.algorithm;

public class Timer {
	private boolean hasBegun = false;
	private long beginTime = 0;
	private long endTime = 0;
	
	/**
	 * begin the timer
	 */
	public void begin() {
		beginTime = System.currentTimeMillis();
		hasBegun = true;
	}
	
	/**
	 * stop the timer
	 */
	public void stop() {
		if (hasBegun) {
			endTime = System.currentTimeMillis();
			hasBegun = false;
		}
	}
	
	public double getTotalTime() {
		double totalTime = (endTime-beginTime)/1000.0;
		return totalTime;
	}
}
