package org.ui.thesis.dtos;

import lombok.Builder;
import lombok.Data;
import org.ui.thesis.enums.StudentLevel;

@Data
@Builder
public class CompiledStudentScore {

	private String studentId;

	private StudentLevel level;

	private Integer originalScore;

	// ChatGPT scores
	private Integer aiScoreChatGptZeroShot;

	private Integer aiScoreChatGptFewShot;

	private Integer aiScoreChatGptCoT;

	// Gemini scores
	private Integer aiScoreGeminiZeroShot;

	private Integer aiScoreGeminiFewShot;

	private Integer aiScoreGeminiCoT;

	// DeepSeek scores
	private Integer aiScoreDeepSeekZeroShot;

	private Integer aiScoreDeepSeekFewShot;

	private Integer aiScoreDeepSeekCoT;

}
