package org.ui.thesis.utils;

import java.util.concurrent.TimeUnit;

public class StopWatchUtil {

	public static String elapsedTimeInSecond(long timeStart, long timeEnd) {
		long elapsedTime = timeEnd - timeStart;
		double rawSeconds = (double) elapsedTime / TimeUnit.SECONDS.toNanos(1);
		return String.format("%.2f", rawSeconds);
	}

}
