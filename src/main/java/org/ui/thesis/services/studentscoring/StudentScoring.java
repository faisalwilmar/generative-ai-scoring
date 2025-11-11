package org.ui.thesis.services.studentscoring;

import org.apache.commons.lang3.tuple.Pair;
import org.ui.thesis.enums.AiModel;
import org.ui.thesis.enums.PromptTechnique;
import org.ui.thesis.services.studentscoring.dto.AiScoringFeedbackDto;
import org.ui.thesis.dtos.AnswerScoreDto;
import org.ui.thesis.dtos.StudentAnswerDto;
import org.ui.thesis.dtos.StudentGradeDto;

import java.util.List;
import java.util.Map;

public interface StudentScoring {

	Pair<Integer, AiScoringFeedbackDto> getAiScoreAndFeedback(AiModel aiModel, PromptTechnique promptTechnique,
			List<AnswerScoreDto> exampleAnswerScore, String scoringGuide, String question, String answer);

	Map<String, List<AnswerScoreDto>> matchResultWithScore(List<StudentAnswerDto> studentAnswers,
			List<StudentGradeDto> studentGrades);

}
