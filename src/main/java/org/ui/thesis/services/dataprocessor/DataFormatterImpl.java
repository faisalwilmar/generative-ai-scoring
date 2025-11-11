package org.ui.thesis.services.dataprocessor;

import lombok.extern.slf4j.Slf4j;
import org.ui.thesis.services.studentscoring.StudentScoring;
import org.ui.thesis.services.studentscoring.StudentScoringImpl;
import org.ui.thesis.dtos.AnswerScoreDto;
import org.ui.thesis.dtos.StudentAnswerDto;
import org.ui.thesis.dtos.StudentGradeDto;
import org.ui.thesis.utils.JsonFileUtil;

import java.util.List;
import java.util.Map;

@Slf4j
public class DataFormatterImpl implements DataFormatter {

	private static final String INPUT_FILE_PATH_ANSWER = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/WRITING TEST/GROUPED RESPONSE (use this)/SEMESTER 6 GABUNGAN FK FKG ELITEP/Accumulated Responses to Use.json";

	private static final String INPUT_FILE_PATH_GRADE = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/WRITING TEST/GROUPED RESPONSE (use this)/SEMESTER 6 GABUNGAN FK FKG ELITEP/Accumulated Grade to Use.json";

	private static final String OUTPUT_FILE_PATH_ANSWER_GRADE = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/WRITING TEST/GROUPED RESPONSE (use this)/SEMESTER 6 GABUNGAN FK FKG ELITEP/Matched Responses to Grade.json";

	@Override
	public void importAndMatchData() {
		log.info("--- 1. IMPORT DATA ---");

		List<StudentAnswerDto> importedStudentAnswerDto = JsonFileUtil.readJsonArrayFromFile(INPUT_FILE_PATH_ANSWER,
				StudentAnswerDto.class);

		if (importedStudentAnswerDto.isEmpty()) {
			log.warn("No Student Answer records imported. Exiting.");
			return;
		}

		log.info("Imported {} answer records.", importedStudentAnswerDto.size());

		List<StudentGradeDto> importedStudentGradeDto = JsonFileUtil.readJsonArrayFromFile(INPUT_FILE_PATH_GRADE,
				StudentGradeDto.class);

		if (importedStudentGradeDto.isEmpty()) {
			log.warn("No Student Grade records imported. Exiting.");
			return;
		}

		log.info("Imported {} grade records.", importedStudentGradeDto.size());

		log.info("\n--- 2. PROCESS DATA ---");

		StudentScoring studentScoringSvc = new StudentScoringImpl();

		Map<String, List<AnswerScoreDto>> processedRecords = studentScoringSvc
			.matchResultWithScore(importedStudentAnswerDto, importedStudentGradeDto);

		log.info("\n--- 3. EXPORT DATA ---");

		JsonFileUtil.writeObjectToFile(processedRecords, OUTPUT_FILE_PATH_ANSWER_GRADE);

		log.info("Exported {} records.", processedRecords.size());
	}

}
