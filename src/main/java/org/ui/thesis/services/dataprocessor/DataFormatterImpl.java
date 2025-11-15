package org.ui.thesis.services.dataprocessor;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.ui.thesis.dtos.AiScoringResult;
import org.ui.thesis.dtos.CompiledStudentScore;
import org.ui.thesis.enums.AiModel;
import org.ui.thesis.enums.PromptTechnique;
import org.ui.thesis.enums.QuestionType;
import org.ui.thesis.services.studentscoring.StudentScoring;
import org.ui.thesis.services.studentscoring.StudentScoringImpl;
import org.ui.thesis.dtos.AnswerScoreDto;
import org.ui.thesis.dtos.StudentAnswerDto;
import org.ui.thesis.dtos.StudentGradeDto;
import org.ui.thesis.utils.JsonFileUtil;
import org.ui.thesis.utils.TextFileUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

	/**
	 * Compilation method with configurable question type filter
	 */
	@Override
	public List<CompiledStudentScore> compileDataForQuestionType(String originalDataPath,
			Map<Pair<AiModel, PromptTechnique>, String> aiResultFiles, QuestionType targetQuestionType) {

		// Step 1: Load original scores
		log.info("Loading original scores from: {}", originalDataPath);
		Map<String, List<AnswerScoreDto>> originalScores = JsonFileUtil.readJsonByReference(originalDataPath,
				new TypeReference<>() {
				});

		if (originalScores == null || originalScores.isEmpty()) {
			log.warn("No original scores found in file: {}", originalDataPath);
			return Collections.emptyList();
		}

		// Step 2: Filter by question type and create student records
		Map<String, CompiledStudentScore.CompiledStudentScoreBuilder> studentMap = new LinkedHashMap<>();

		for (AnswerScoreDto score : originalScores.values().stream().flatMap(List::stream).toList()) {
			// Filter by question type
			if (score.getQuestionType() != targetQuestionType) {
				continue;
			}

			String studentId = score.getStudentId();

			// Create or update student record - no aggregation, just direct assignment
			studentMap.put(studentId,
					CompiledStudentScore.builder()
						.studentId(studentId)
						.level(score.getLevel())
						.originalScore(score.getOriginalScore()));
		}

		log.info("Loaded {} student records for {}", studentMap.size(), targetQuestionType);

		// Step 3: Load AI scores from result files
		for (Map.Entry<Pair<AiModel, PromptTechnique>, String> entry : aiResultFiles.entrySet()) {
			Pair<AiModel, PromptTechnique> key = entry.getKey();
			String filePath = entry.getValue();

			log.info("Loading AI scores from: {}", filePath);

			List<AiScoringResult> aiResults = JsonFileUtil.readJsonArrayFromFile(filePath, AiScoringResult.class);

			if (aiResults.isEmpty()) {
				log.warn("No AI scores found in file: {}", filePath);
				continue;
			}

			// Process each AI result - filter by question type
			int matchedCount = 0;
			for (AiScoringResult aiResult : aiResults) {
				// Filter by question type
				if (aiResult.getQuestionType() != targetQuestionType) {
					continue;
				}

				matchedCount++;
				String studentId = aiResult.getStudentId();

				if (!studentMap.containsKey(studentId)) {
					log.warn("Student {} found in AI results but not in original scores for {}", studentId,
							targetQuestionType);
					// Still create entry for this student
					studentMap.put(studentId,
							CompiledStudentScore.builder().studentId(studentId).level(aiResult.getLevel()));
				}

				CompiledStudentScore.CompiledStudentScoreBuilder builder = studentMap.get(studentId);
				updateAiScore(builder, key, aiResult.getAiScore());
			}

			log.info("Matched {} records for {} from {}", matchedCount, targetQuestionType, filePath);
		}

		// Step 4: Build final list
		List<CompiledStudentScore> result = studentMap.values()
			.stream()
			.map(CompiledStudentScore.CompiledStudentScoreBuilder::build)
			.collect(Collectors.toList());

		log.info("Compilation complete. Total students: {}", result.size());
		return result;
	}

	/**
	 * Update AI score based on model and technique No aggregation - direct assignment
	 */
	private static void updateAiScore(CompiledStudentScore.CompiledStudentScoreBuilder builder,
			Pair<AiModel, PromptTechnique> key, Integer score) {

		switch (key.getLeft()) {
			case GEMINI -> {
				switch (key.getRight()) {
					case ZERO_SHOT -> builder.aiScoreGeminiZeroShot(score);
					case FEW_SHOT -> builder.aiScoreGeminiFewShot(score);
					case CHAIN_OF_THOUGHT -> builder.aiScoreGeminiCoT(score);
					default -> log.warn("Unknown AI model/technique key: {} {}", key.getLeft(), key.getRight());
				}
			}
			case CHATGPT -> {
				switch (key.getRight()) {
					case ZERO_SHOT -> builder.aiScoreChatGptZeroShot(score);
					case FEW_SHOT -> builder.aiScoreChatGptFewShot(score);
					case CHAIN_OF_THOUGHT -> builder.aiScoreChatGptCoT(score);
					default -> log.warn("Unknown AI model/technique key: {} {}", key.getLeft(), key.getRight());
				}
			}
			case DEEPSEEK -> {
				switch (key.getRight()) {
					case ZERO_SHOT -> builder.aiScoreDeepSeekZeroShot(score);
					case FEW_SHOT -> builder.aiScoreDeepSeekFewShot(score);
					case CHAIN_OF_THOUGHT -> builder.aiScoreDeepSeekCoT(score);
					default -> log.warn("Unknown AI model/technique key: {} {}", key.getLeft(), key.getRight());
				}
			}
			default -> log.warn("Unknown AI model/technique key: {} {}", key.getLeft(), key.getRight());

		}
	}

	/**
	 * Export compiled data to CSV with semicolon delimiter
	 */
	@Override
	public void exportToCSV(List<CompiledStudentScore> scores, String outputPath) {
		List<String> lines = new ArrayList<>();

		// Header
		lines.add(String.join(";", "studentId", "level", "originalScore", "aiScore_chatGpt_zeroShot",
				"aiScore_chatGpt_fewShot", "aiScore_chatGpt_coT", "aiScore_gemini_zeroShot", "aiScore_gemini_fewShot",
				"aiScore_gemini_coT", "aiScore_deepSeek_zeroShot", "aiScore_deepSeek_fewShot", "aiScore_deepSeek_coT",
				"absDiff_chatGpt_zeroShot", "absDiff_chatGpt_fewShot", "absDiff_chatGpt_coT", "absDiff_gemini_zeroShot",
				"absDiff_gemini_fewShot", "absDiff_gemini_coT", "absDiff_deepSeek_zeroShot", "absDiff_deepSeek_fewShot",
				"absDiff_deepSeek_coT"));

		// Data rows
		for (CompiledStudentScore score : scores) {
			lines.add(String.join(";", nvl(score.getStudentId()), nvl(score.getLevel()), nvl(score.getOriginalScore()),
					nvl(score.getAiScoreChatGptZeroShot()), nvl(score.getAiScoreChatGptFewShot()),
					nvl(score.getAiScoreChatGptCoT()), nvl(score.getAiScoreGeminiZeroShot()),
					nvl(score.getAiScoreGeminiFewShot()), nvl(score.getAiScoreGeminiCoT()),
					nvl(score.getAiScoreDeepSeekZeroShot()), nvl(score.getAiScoreDeepSeekFewShot()),
					nvl(score.getAiScoreDeepSeekCoT()),
					nvl(getAbsoluteValue(score.getOriginalScore(), score.getAiScoreChatGptZeroShot())),
					nvl(getAbsoluteValue(score.getOriginalScore(), score.getAiScoreChatGptFewShot())),
					nvl(getAbsoluteValue(score.getOriginalScore(), score.getAiScoreChatGptCoT())),
					nvl(getAbsoluteValue(score.getOriginalScore(), score.getAiScoreGeminiZeroShot())),
					nvl(getAbsoluteValue(score.getOriginalScore(), score.getAiScoreGeminiFewShot())),
					nvl(getAbsoluteValue(score.getOriginalScore(), score.getAiScoreGeminiCoT())),
					nvl(getAbsoluteValue(score.getOriginalScore(), score.getAiScoreDeepSeekZeroShot())),
					nvl(getAbsoluteValue(score.getOriginalScore(), score.getAiScoreDeepSeekFewShot())),
					nvl(getAbsoluteValue(score.getOriginalScore(), score.getAiScoreDeepSeekCoT()))));
		}

		// Write to file
		TextFileUtil.writeLines(lines, outputPath);
		log.info("Successfully exported {} records to {}", scores.size(), outputPath);
	}

	/**
	 * Helper method to convert null values to empty string
	 */
	private static String nvl(Object value) {
		return value != null ? value.toString() : "";
	}

	private Integer getAbsoluteValue(Integer orginalScore, Integer aiScore) {
		if (orginalScore == null || aiScore == null)
			return null;
		return Math.abs(orginalScore - aiScore);
	}

}
