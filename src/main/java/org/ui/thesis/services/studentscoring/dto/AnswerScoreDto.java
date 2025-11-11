package org.ui.thesis.services.studentscoring.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ui.thesis.enums.QuestionType;
import org.ui.thesis.enums.StudentLevel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnswerScoreDto {

	public String semester;

	public String faculty;

	public StudentLevel level;

	public String fullName;

	public String studentId;

	public String email;

	public QuestionType questionType;

	public String answer;

	public int finalScore;

	public int originalScore;

	public String scoreReasoning;

}
