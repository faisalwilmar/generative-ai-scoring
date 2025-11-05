package org.ui.thesis.services.studentscoring;

import org.ui.thesis.services.studentscoring.dto.AnswerScoreDto;
import org.ui.thesis.services.studentscoring.dto.StudentAnswerDto;
import org.ui.thesis.services.studentscoring.dto.StudentGradeDto;

import java.util.List;
import java.util.Map;

public interface StudentScoring {
    Map<String, List<AnswerScoreDto>> matchResultWithScore(List<StudentAnswerDto> studentAnswers, List<StudentGradeDto> studentGrades);
}
