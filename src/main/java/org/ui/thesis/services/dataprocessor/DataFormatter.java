package org.ui.thesis.services.dataprocessor;

import org.apache.commons.lang3.tuple.Pair;
import org.ui.thesis.dtos.CompiledStudentScore;
import org.ui.thesis.enums.AiModel;
import org.ui.thesis.enums.PromptTechnique;
import org.ui.thesis.enums.QuestionType;

import java.util.List;
import java.util.Map;

public interface DataFormatter {

	void importAndMatchData();

	List<CompiledStudentScore> compileDataForQuestionType(String originalDataPath,
			Map<Pair<AiModel, PromptTechnique>, String> aiResultFiles, QuestionType targetQuestionType);

	void exportToCSV(List<CompiledStudentScore> scores, String outputPath);

}
