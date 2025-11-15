package org.ui.thesis.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ui.thesis.enums.AiModel;
import org.ui.thesis.enums.PromptTechnique;
import org.ui.thesis.enums.QuestionType;
import org.ui.thesis.enums.StudentLevel;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiScoringResult {

	private String semester;

	private String faculty;

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	private StudentLevel level;

	private String fullName;

	private String studentId;

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	private QuestionType questionType;

	private String answer;

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	private AiModel aiModel;

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	private PromptTechnique promptTechnique;

	private int aiScore;

	private String aiFeedback;

	private Integer tokenUsage;

}
