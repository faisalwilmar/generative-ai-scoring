package org.ui.thesis.enums;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum QuestionType {

	QUESTION_1(5), QUESTION_2(5), QUESTION_3(15), QUESTION_4(22);

	private final int multiplier;

	QuestionType(int multiplier) {
		this.multiplier = multiplier;
	}

	public int getOriginalScore(double convertedScore) {
		return ((int) convertedScore / multiplier);
	}

}
