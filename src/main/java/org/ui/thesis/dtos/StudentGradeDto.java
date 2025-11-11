package org.ui.thesis.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ui.thesis.enums.StudentLevel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGradeDto {

	private String semester;

	private String faculty;

	private StudentLevel level;

	private String fullName;

	private String studentId;

	private String email;

	private String gradeOf200;

	private String gradeQuestion1Of15;

	private String gradeQuestion2Of15;

	private String gradeQuestion3Of60;

	private String gradeQuestion4Of110;

	public int getQuestion1Score() {
		try {
			return Integer.parseInt(gradeQuestion1Of15.trim());
		}
		catch (NumberFormatException e) {
			return 0;
		}
	}

	public int getQuestion2Score() {
		try {
			return Integer.parseInt(gradeQuestion2Of15.trim());
		}
		catch (NumberFormatException e) {
			return 0;
		}
	}

	public int getQuestion3Score() {
		try {
			return Integer.parseInt(gradeQuestion3Of60.trim());
		}
		catch (NumberFormatException e) {
			return 0;
		}
	}

	public int getQuestion4Score() {
		try {
			return Integer.parseInt(gradeQuestion4Of110.trim());
		}
		catch (NumberFormatException e) {
			return 0;
		}
	}

}
