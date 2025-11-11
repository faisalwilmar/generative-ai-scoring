package org.ui.thesis.services.studentscoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ui.thesis.enums.StudentLevel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAnswerDto {

	private String semester;

	private String faculty;

	private StudentLevel level;

	private String fullName;

	private String studentId;

	private String email;

	private String responseQuestion1;

	private String responseQuestion2;

	private String responseQuestion3;

	private String responseQuestion4;

}
