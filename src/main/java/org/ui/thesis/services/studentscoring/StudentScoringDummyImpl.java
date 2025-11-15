package org.ui.thesis.services.studentscoring;

import org.apache.commons.lang3.tuple.Pair;
import org.ui.thesis.dtos.AnswerScoreDto;
import org.ui.thesis.dtos.StudentAnswerDto;
import org.ui.thesis.dtos.StudentGradeDto;
import org.ui.thesis.enums.AiModel;
import org.ui.thesis.enums.PromptTechnique;
import org.ui.thesis.services.studentscoring.dto.AiScoringFeedbackDto;

import java.util.List;
import java.util.Map;

public class StudentScoringDummyImpl implements StudentScoring {

	@Override
	public Pair<Integer, AiScoringFeedbackDto> getAiScoreAndFeedback(AiModel aiModel, PromptTechnique promptTechnique,
			List<AnswerScoreDto> exampleAnswerScore, String scoringGuide, String question, String answer) {
		AiScoringFeedbackDto scoringFeedbackDto = new AiScoringFeedbackDto(0, "Just my wild thoughts");
		return Pair.of(0, scoringFeedbackDto);
	}

	@Override
	public Map<String, List<AnswerScoreDto>> matchResultWithScore(List<StudentAnswerDto> studentAnswers,
			List<StudentGradeDto> studentGrades) {
		return Map.of();
	}

}
