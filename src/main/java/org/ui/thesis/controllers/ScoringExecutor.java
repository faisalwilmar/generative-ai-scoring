package org.ui.thesis.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.ui.thesis.dtos.AiScoringResult;
import org.ui.thesis.dtos.AnswerScoreDto;
import org.ui.thesis.enums.AiModel;
import org.ui.thesis.enums.PromptTechnique;
import org.ui.thesis.enums.QuestionType;
import org.ui.thesis.exceptions.ConcurrentException;
import org.ui.thesis.services.dataprocessor.DataFormatter;
import org.ui.thesis.services.studentscoring.StudentScoring;
import org.ui.thesis.services.studentscoring.dto.AiScoringFeedbackDto;
import org.ui.thesis.utils.JsonFileUtil;
import org.ui.thesis.utils.TextFileUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.ui.thesis.utils.ConcurrentTaskUtil.runAllTasks;

@Slf4j
@RequiredArgsConstructor
public class ScoringExecutor {

	private final StudentScoring studentScoring;

	private final DataFormatter dataFormatter;

	private static final String INPUT_EXAMPLE_FILE_PATH_FEW_SHOT = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Result/Few Shot Sample/Few Shot Example Prompt.json";

	private static final String INPUT_QUESTION_3_FILE_PATH = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Question 3 Advanced.txt";

	private static final String INPUT_QUESTION_3_SCORING_GUIDE_FILE_PATH = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Question 3 Scoring Guide.txt";

	private static final String OUTPUT_ERROR_DEFAULT_FOLDER_PATH = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Result";

	public void ExecuteQuestion3Scoring(ConcurrentLinkedQueue<Exception> errorQueue, String answerScoreFilePath,
			String feedbackOutputFilePath, AiModel aiModel, PromptTechnique promptTechnique) {
		Map<String, List<AnswerScoreDto>> processedRecords = JsonFileUtil.readJsonByReference(answerScoreFilePath,
				new TypeReference<>() {
				});

		List<AnswerScoreDto> exampleFewShot;

		if (promptTechnique.equals(PromptTechnique.FEW_SHOT)
				|| promptTechnique.equals(PromptTechnique.CHAIN_OF_THOUGHT)) {
			exampleFewShot = JsonFileUtil.readJsonArrayFromFile(INPUT_EXAMPLE_FILE_PATH_FEW_SHOT, AnswerScoreDto.class);
		}
		else {
			exampleFewShot = new ArrayList<>();
		}

		String scoringGuide = TextFileUtil.readAllText(INPUT_QUESTION_3_SCORING_GUIDE_FILE_PATH);

		String question = TextFileUtil.readAllText(INPUT_QUESTION_3_FILE_PATH);

		List<AiScoringResult> feedbackRecords = new ArrayList<>();

		List<AnswerScoreDto> answerScoreQuestion3List = Objects.requireNonNull(processedRecords)
			.values()
			.stream()
			.filter(Objects::nonNull)
			.flatMap(List::stream)
			.filter(dto -> QuestionType.QUESTION_3.equals(dto.getQuestionType()))
			.toList();

		// The Java equivalent of ConcurrentBag<Exception>
		ConcurrentLinkedQueue<Exception> sharedErrorQueue = new ConcurrentLinkedQueue<>();
		Collection<Callable<AiScoringResult>> tasks = new ArrayList<>();

		for (AnswerScoreDto answerScoreDto : answerScoreQuestion3List) {
			tasks.add(() -> {
				try {
					Pair<Integer, AiScoringFeedbackDto> feedbackDtoMap = studentScoring.getAiScoreAndFeedback(aiModel,
							promptTechnique, exampleFewShot, scoringGuide, question, answerScoreDto.getAnswer());
					AiScoringFeedbackDto feedbackDto = feedbackDtoMap.getRight();
					return AiScoringResult.builder()
						.semester(answerScoreDto.getSemester())
						.faculty(answerScoreDto.getFaculty())
						.level(answerScoreDto.getLevel())
						.fullName(answerScoreDto.getFullName())
						.studentId(answerScoreDto.getStudentId())
						.questionType(QuestionType.QUESTION_3)
						.answer(answerScoreDto.getAnswer())
						.aiModel(aiModel)
						.promptTechnique(promptTechnique)
						.aiScore(feedbackDto.getLlm_grade())
						.aiFeedback(feedbackDto.getLlm_feedback())
						.tokenUsage(feedbackDtoMap.getLeft())
						.build();
				}
				catch (Exception ex) {
					ConcurrentException concurrentException = new ConcurrentException(answerScoreDto.studentId,
							ex.getMessage(), ex);
					sharedErrorQueue.add(concurrentException);
					return new AiScoringResult();
				}
			});
		}

		ExecutorService executor = Executors.newFixedThreadPool(4);

		try {
			List<Future<AiScoringResult>> futures = runAllTasks(tasks, sharedErrorQueue, executor);

			int successCount = 0;
			for (Future<AiScoringResult> future : futures) {
				// Check if the future finished without an uncaught exception
				if (future.isDone() && !future.isCancelled()) {
					try {
						AiScoringResult scoringResult = future.get();
						// Check if scoring result is not default object returned when
						// error
						if (scoringResult.getStudentId() != null && !scoringResult.getStudentId().isBlank()) {
							feedbackRecords.add(future.get());
							successCount++;
						}
					}
					catch (Exception e) {
						log.error("Unusual Error :: {}", e.getMessage());
					}
				}
			}

			int failedCount = sharedErrorQueue.size();

			log.info("Total Tasks Run: {}", tasks.size());
			log.info("Total Successful: {}", successCount);
			log.info("Total Failed: {}", failedCount);

			if (failedCount > 0) {
				StringBuilder stringBuilder = new StringBuilder();
				log.warn("FAILURE DETAILS:");
				int count = 1;
				for (Exception e : sharedErrorQueue) {
					log.error("{}. {}", count++, e.getMessage());
					if (e instanceof ConcurrentException concurrentEx) {
						stringBuilder.append(concurrentEx.getOperationId())
							.append("::")
							.append(concurrentEx.getMessage())
							.append("\n");
					}
				}

				TextFileUtil.writeAllText(stringBuilder.toString(), OUTPUT_ERROR_DEFAULT_FOLDER_PATH + "/Error.txt");
			}
			else {
				log.info("Processing completed with 0 errors.");
			}

		}
		catch (InterruptedException e) {
			log.error("Main thread interrupted.");
			Thread.currentThread().interrupt();
		}
		finally {
			// Standard Executor shutdown boilerplate
			executor.shutdown();
		}

		JsonFileUtil.writeObjectToFile(feedbackRecords, feedbackOutputFilePath);
	}

}
