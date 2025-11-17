package org.ui.thesis.utils;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;

@Slf4j
public class CommonUtil {

	/**
	 * Play system beep sound multiple times
	 * @param times Number of beeps
	 */
	public static void beep(int times) {

		beep(times, 1500);
	}

	/**
	 * Play system beep sound multiple times
	 * @param times Number of beeps
	 */
	public static void beep(int times, long pauseMillis) {

		try {
			for (int i = 0; i < times; i++) {
				Toolkit.getDefaultToolkit().beep();
				if (times > 1 && i < times - 1) {
					Thread.sleep(pauseMillis); // Pause between beeps
				}
			}
		}
		catch (Exception e) {
			// Silently fail if beep not available
			log.debug("Beep sound not available: {}", e.getMessage());
		}
	}

}
