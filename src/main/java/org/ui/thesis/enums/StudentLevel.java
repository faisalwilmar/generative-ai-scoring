package org.ui.thesis.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum StudentLevel {

	BASIC_ELEMENTARY("BASIC ELEMENTARY"), INTERMEDIATE("INTERMEDIATE"), ADVANCED_PROFICIENT("ADVANCED PROFICIENT");

	private final String levelDescription;

	StudentLevel(String levelDescription) {
		this.levelDescription = levelDescription;
	}

	@JsonCreator
	public static StudentLevel fromDescription(String description) {
		if (description == null) {
			throw new IllegalArgumentException("Level description cannot be null.");
		}

		for (StudentLevel level : StudentLevel.values()) {
			if (level.levelDescription.equalsIgnoreCase(description)) {
				return level;
			}
		}

		throw new IllegalArgumentException("Unknown student level description: " + description);
	}

}
