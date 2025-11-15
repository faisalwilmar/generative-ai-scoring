package org.ui.thesis.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum StudentLevel {

	BASIC_ELEMENTARY("BASIC ELEMENTARY"), INTERMEDIATE("INTERMEDIATE"), ADVANCED_PROFICIENT("ADVANCED PROFICIENT");

	private final String levelDescription;

	StudentLevel(String levelDescription) {
		this.levelDescription = levelDescription;
	}

	@JsonCreator
	public static StudentLevel fromString(String value) {
		try {
			return StudentLevel.valueOf(value);
		}
		catch (IllegalArgumentException e) {
			// Fallback to description matching
			for (StudentLevel level : StudentLevel.values()) {
				if (level.levelDescription.equalsIgnoreCase(value)) {
					return level;
				}
			}
			throw new IllegalArgumentException("Unknown student level: " + value);
		}
	}

}
