package org.ui.thesis.services.studentscoring.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ui.thesis.enums.QuestionType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnswerScoreDto {

	public String semester;

	public String faculty;

	public String level;

	public String fullName;

	public String studentId;

	public String email;

	public QuestionType questionType;

	public String answer;

	public int finalScore;

	public int originalScore;

}
