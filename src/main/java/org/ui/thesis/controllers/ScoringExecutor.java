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
import org.ui.thesis.services.studentscoring.StudentScoring;
import org.ui.thesis.services.studentscoring.dto.AiScoringFeedbackDto;
import org.ui.thesis.utils.JsonFileUtil;
import org.ui.thesis.utils.StopWatchUtil;
import org.ui.thesis.utils.TextFileUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
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

	private static final String INPUT_EXAMPLE_FILE_PATH_FEW_SHOT = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Result/Few Shot Sample/Few Shot Example Prompt.json";

	private static final String INPUT_EXAMPLE_FILE_PATH_CHAIN_OF_THOUGHT = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Result/Chain of Thought Sample/Chain of Thought Example Prompt.json";

	private static final String INPUT_QUESTION_BASIC_3_FILE_PATH = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Question 3 Basic.txt";

	private static final String INPUT_QUESTION_INTERMEDIATE_3_FILE_PATH = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Question 3 Intermediate.txt";

	private static final String INPUT_QUESTION_ADVANCED_3_FILE_PATH = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Question 3 Advanced.txt";

	private static final String INPUT_QUESTION_3_SCORING_GUIDE_FILE_PATH = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Question 3 Scoring Guide.txt";

	public void ExecuteQuestion3Scoring(ConcurrentLinkedQueue<Exception> errorQueue, String answerScoreFilePath,
			String feedbackOutputFilePath, AiModel aiModel, PromptTechnique promptTechnique) {

		Map<String, List<AnswerScoreDto>> processedRecords = JsonFileUtil.readJsonByReference(answerScoreFilePath,
				new TypeReference<>() {
				});

		List<AnswerScoreDto> exampleShot;

		if (promptTechnique.equals(PromptTechnique.FEW_SHOT)) {
			exampleShot = JsonFileUtil.readJsonArrayFromFile(INPUT_EXAMPLE_FILE_PATH_FEW_SHOT, AnswerScoreDto.class);
		}
		else if (promptTechnique.equals(PromptTechnique.CHAIN_OF_THOUGHT)) {
			exampleShot = JsonFileUtil.readJsonArrayFromFile(INPUT_EXAMPLE_FILE_PATH_CHAIN_OF_THOUGHT,
					AnswerScoreDto.class);
		}
		else {
			exampleShot = new ArrayList<>();
		}

		String scoringGuide = TextFileUtil.readAllText(INPUT_QUESTION_3_SCORING_GUIDE_FILE_PATH);

		List<AiScoringResult> feedbackRecords = new ArrayList<>();

		List<AnswerScoreDto> answerScoreQuestion3List = Objects.requireNonNull(processedRecords)
			.values()
			.stream()
			.filter(Objects::nonNull)
			.flatMap(List::stream)
			.filter(dto -> QuestionType.QUESTION_3.equals(dto.getQuestionType()))
			.limit(10)
			.toList();

		Collection<Callable<AiScoringResult>> tasks = new ArrayList<>();

		for (AnswerScoreDto answerScoreDto : answerScoreQuestion3List) {
			tasks.add(() -> {
				try {
					long timeStart = System.nanoTime();

					log.info("[{}][Start]", answerScoreDto.getStudentId());
					List<AnswerScoreDto> relatedExample = exampleShot.stream()
						.filter(x -> x.getLevel().equals(answerScoreDto.getLevel()))
						.toList();
					String relatedQuestion = switch (answerScoreDto.getLevel()) {
						case BASIC_ELEMENTARY -> TextFileUtil.readAllText(INPUT_QUESTION_BASIC_3_FILE_PATH);
						case INTERMEDIATE -> TextFileUtil.readAllText(INPUT_QUESTION_INTERMEDIATE_3_FILE_PATH);
						case ADVANCED_PROFICIENT -> TextFileUtil.readAllText(INPUT_QUESTION_ADVANCED_3_FILE_PATH);
					};
					Pair<Integer, AiScoringFeedbackDto> feedbackDtoMap = studentScoring.getAiScoreAndFeedback(aiModel,
							promptTechnique, relatedExample, scoringGuide, relatedQuestion, answerScoreDto.getAnswer());
					AiScoringFeedbackDto feedbackDto = feedbackDtoMap.getRight();
					log.info("[{}][Result Received]", answerScoreDto.getStudentId());

					long timeEnd = System.nanoTime();
					log.info("[{}][Time Elapsed][{} Seconds]", answerScoreDto.getStudentId(),
							StopWatchUtil.elapsedTimeInSecond(timeStart, timeEnd));

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
					errorQueue.add(concurrentException);
					log.info("[{}][Error]", answerScoreDto.getStudentId());
					return new AiScoringResult();
				}
			});
		}

		ExecutorService executor = Executors.newFixedThreadPool(1);

		try {
			List<Future<AiScoringResult>> futures = runAllTasks(tasks, errorQueue, executor);

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

			int failedCount = errorQueue.size();

			log.info("Total Tasks Run: {}", tasks.size());
			log.info("Total Successful: {}", successCount);
			log.info("Total Failed: {}", failedCount);

			if (failedCount > 0) {
				StringBuilder stringBuilder = new StringBuilder();
				log.warn("FAILURE DETAILS:");
				int count = 1;
				for (Exception e : errorQueue) {
					log.error("{}. {}", count++, e.getMessage());
					if (e instanceof ConcurrentException concurrentEx) {
						stringBuilder.append(concurrentEx.getOperationId())
							.append("::")
							.append(concurrentEx.getMessage())
							.append("\n");
					}
				}

				TextFileUtil.writeAllText(stringBuilder.toString(), getErrorFilePath(feedbackOutputFilePath));
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

	private String getErrorFilePath(String originalPath) {
		Path path = Paths.get(originalPath);
		String filename = path.getFileName().toString();

		// Replace .json with _Error.json
		String newFilename = filename.replace(".json", "_Error.json");

		return path.getParent().resolve(newFilename).toString();
	}

}
