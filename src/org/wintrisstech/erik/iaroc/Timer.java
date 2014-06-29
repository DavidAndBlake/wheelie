package org.wintrisstech.erik.iaroc;

public class Timer {

	private long startTime;

	private long howLong;

	public Timer(long howLong) {

		this.howLong = howLong;
		startTime = System.currentTimeMillis();
	}

	public boolean isExpired() {

		long currentTime = System.currentTimeMillis();
		long completionTime = startTime + howLong;
		return currentTime >= completionTime;
	}
}
