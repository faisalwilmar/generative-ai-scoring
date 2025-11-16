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

@Slf4j
@RequiredArgsConstructor
public class ScoringExecutor {

	private final StudentScoring studentScoring;

	private static final String INPUT_EXAMPLE_FILE_PATH_FEW_SHOT = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Few Shot Example Prompt.json";

	private static final String INPUT_EXAMPLE_FILE_PATH_CHAIN_OF_THOUGHT = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Chain of Thought Example Prompt.json";

	private static final String INPUT_QUESTION_BASIC_3_FILE_PATH = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Question 3 Basic.txt";

	private static final String INPUT_QUESTION_INTERMEDIATE_3_FILE_PATH = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Question 3 Intermediate.txt";

	private static final String INPUT_QUESTION_ADVANCED_3_FILE_PATH = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Question 3 Advanced.txt";

	private static final String INPUT_QUESTION_3_SCORING_GUIDE_FILE_PATH = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Question 3 Scoring Guide.txt";

	/**
	 * Execute Question 3 scoring. The original logic from the Author.
	 * @param errorQueue The queue to collect errors
	 * @param answerScoreFilePath Path to input answer scores
	 * @param feedbackOutputFilePath Path to output feedback results
	 * @param aiModel The AI model to use
	 * @param promptTechnique The prompting technique
	 */
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

	/**
	 * Execute Question 3 scoring with configurable retry parameters
	 * @param errorQueue The queue to collect errors
	 * @param answerScoreFilePath Path to input answer scores
	 * @param feedbackOutputFilePath Path to output feedback results
	 * @param aiModel The AI model to use
	 * @param promptTechnique The prompting technique
	 * @param maxRetryAttempts Maximum number of retry attempts for failed tasks
	 * @param retryWaitSeconds Wait time in seconds between retry attempts
	 */
	public void ExecuteQuestion3ScoringWithRetry(ConcurrentLinkedQueue<Exception> errorQueue,
			String answerScoreFilePath, String feedbackOutputFilePath, AiModel aiModel, PromptTechnique promptTechnique,
			int maxRetryAttempts, int retryWaitSeconds) {

		Map<String, List<AnswerScoreDto>> processedRecords = JsonFileUtil.readJsonByReference(answerScoreFilePath,
				new TypeReference<>() {
				});

		List<AnswerScoreDto> exampleShot = loadExampleShots(promptTechnique);
		String scoringGuide = TextFileUtil.readAllText(INPUT_QUESTION_3_SCORING_GUIDE_FILE_PATH);

		List<AnswerScoreDto> answerScoreQuestion3List = Objects.requireNonNull(processedRecords)
			.values()
			.stream()
			.filter(Objects::nonNull)
			.flatMap(List::stream)
			.filter(dto -> QuestionType.QUESTION_3.equals(dto.getQuestionType()))
			.toList();

		List<AiScoringResult> feedbackRecords = new ArrayList<>();

		// Initial execution
		log.info("=".repeat(30));
		log.info("Starting initial execution for {} students", answerScoreQuestion3List.size());
		log.info("AI Model: {}, Technique: {}", aiModel, promptTechnique);
		log.info("Max Retry Attempts: {}, Retry Wait: {}s", maxRetryAttempts, retryWaitSeconds);
		log.info("=".repeat(30));

		List<AnswerScoreDto> remainingTasks = new ArrayList<>(answerScoreQuestion3List);
		int attemptNumber = 1;

		while (!remainingTasks.isEmpty() && attemptNumber <= maxRetryAttempts + 1) {
			if (attemptNumber > 1) {
				log.info("");
				log.info("=".repeat(30));
				log.info("RETRY ATTEMPT {} of {}", attemptNumber - 1, maxRetryAttempts);
				log.info("Retrying {} failed tasks", remainingTasks.size());
				log.info("Waiting {}s before retry...", retryWaitSeconds);
				log.info("=".repeat(30));

				try {
					Thread.sleep(retryWaitSeconds * 1000L);
				}
				catch (InterruptedException e) {
					log.error("Retry wait interrupted");
					Thread.currentThread().interrupt();
					break;
				}
			}

			// Clear error queue for this attempt
			errorQueue.clear();

			// Execute tasks
			ExecutionResult result = executeTasks(remainingTasks, exampleShot, scoringGuide, aiModel, promptTechnique,
					errorQueue);

			// Add successful results
			feedbackRecords.addAll(result.successfulResults);

			// Prepare for next iteration
			remainingTasks = result.failedStudents;

			// Log attempt summary
			log.info("");
			log.info("-".repeat(80));
			log.info("Attempt {} Summary:", attemptNumber);
			log.info("  Successful: {}", result.successfulResults.size());
			log.info("  Failed: {}", result.failedStudents.size());
			log.info("-".repeat(80));

			// Break if all tasks succeeded
			if (remainingTasks.isEmpty()) {
				log.info("All tasks completed successfully!");
				break;
			}

			// Check if we should continue retrying
			if (attemptNumber > maxRetryAttempts) {
				log.warn("Maximum retry attempts ({}) reached. {} tasks still failed.", maxRetryAttempts,
						remainingTasks.size());
				break;
			}

			attemptNumber++;
		}

		// Final summary
		logFinalSummary(answerScoreQuestion3List.size(), feedbackRecords.size(), remainingTasks.size(), errorQueue,
				feedbackOutputFilePath);

		// Write results
		JsonFileUtil.writeObjectToFile(feedbackRecords, feedbackOutputFilePath);
	}

	/**
	 * Execute tasks for a list of students
	 */
	private ExecutionResult executeTasks(List<AnswerScoreDto> students, List<AnswerScoreDto> exampleShot,
			String scoringGuide, AiModel aiModel, PromptTechnique promptTechnique,
			ConcurrentLinkedQueue<Exception> errorQueue) {

		Collection<Callable<TaskResult>> tasks = new ArrayList<>();

		for (AnswerScoreDto answerScoreDto : students) {
			tasks.add(
					() -> executeTask(answerScoreDto, exampleShot, scoringGuide, aiModel, promptTechnique, errorQueue));
		}

		ExecutorService executor = Executors.newFixedThreadPool(1);
		List<AiScoringResult> successfulResults = new ArrayList<>();
		List<AnswerScoreDto> failedStudents = new ArrayList<>();

		try {
			List<Future<TaskResult>> futures = runAllTasks(tasks, errorQueue, executor);

			for (Future<TaskResult> future : futures) {
				if (future.isDone() && !future.isCancelled()) {
					try {
						TaskResult taskResult = future.get();

						if (taskResult.success()) {
							successfulResults.add(taskResult.scoringResult());
						}
						else {
							failedStudents.add(taskResult.originalDto());
						}
					}
					catch (Exception e) {
						log.error("Unusual error getting future result: {}", e.getMessage());
					}
				}
			}
		}
		catch (InterruptedException e) {
			log.error("Task execution interrupted");
			Thread.currentThread().interrupt();
		}
		finally {
			executor.shutdown();
		}

		return new ExecutionResult(successfulResults, failedStudents);
	}

	/**
	 * Execute a single scoring task
	 */
	private TaskResult executeTask(AnswerScoreDto answerScoreDto, List<AnswerScoreDto> exampleShot, String scoringGuide,
			AiModel aiModel, PromptTechnique promptTechnique, ConcurrentLinkedQueue<Exception> errorQueue) {

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

			AiScoringResult result = AiScoringResult.builder()
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

			return TaskResult.success(result, answerScoreDto);

		}
		catch (Exception ex) {
			ConcurrentException concurrentException = new ConcurrentException(answerScoreDto.getStudentId(),
					ex.getMessage(), ex);
			errorQueue.add(concurrentException);
			log.error("[{}][Error] {}", answerScoreDto.getStudentId(), ex.getMessage());
			return TaskResult.failure(answerScoreDto);
		}
	}

	/**
	 * Load example shots based on prompt technique
	 */
	private List<AnswerScoreDto> loadExampleShots(PromptTechnique promptTechnique) {
		if (promptTechnique.equals(PromptTechnique.FEW_SHOT)) {
			return JsonFileUtil.readJsonArrayFromFile(INPUT_EXAMPLE_FILE_PATH_FEW_SHOT, AnswerScoreDto.class);
		}
		else if (promptTechnique.equals(PromptTechnique.CHAIN_OF_THOUGHT)) {
			return JsonFileUtil.readJsonArrayFromFile(INPUT_EXAMPLE_FILE_PATH_CHAIN_OF_THOUGHT, AnswerScoreDto.class);
		}
		else {
			return new ArrayList<>();
		}
	}

	/**
	 * Log final summary and write error file if needed
	 */
	private void logFinalSummary(int totalTasks, int successCount, int failedCount,
			ConcurrentLinkedQueue<Exception> errorQueue, String feedbackOutputFilePath) {

		log.info("");
		log.info("=".repeat(30));
		log.info("FINAL EXECUTION SUMMARY");
		log.info("=".repeat(30));
		log.info("Total Tasks: {}", totalTasks);
		log.info("Successful: {}", successCount);
		log.info("Failed: {}", failedCount);
		log.info("Success Rate: {}", String.format("%.2f", (successCount * 100.0 / totalTasks)));
		log.info("=".repeat(30));

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

			String errorFilePath = getErrorFilePath(feedbackOutputFilePath);
			TextFileUtil.writeAllText(stringBuilder.toString(), errorFilePath);
			log.error("Error details written to: {}", errorFilePath);
		}
		else {
			log.info("Processing completed with 0 errors!");
		}
	}

	/**
	 * Result of executing tasks for a batch of students
	 */
	private record ExecutionResult(List<AiScoringResult> successfulResults, List<AnswerScoreDto> failedStudents) {
	}

	/**
	 * Result of a single task execution
	 */
	private record TaskResult(boolean success, AiScoringResult scoringResult, AnswerScoreDto originalDto) {

		static TaskResult success(AiScoringResult result, AnswerScoreDto dto) {
			return new TaskResult(true, result, dto);
		}

		static TaskResult failure(AnswerScoreDto dto) {
			return new TaskResult(false, null, dto);
		}

	}

	// Placeholder for the existing runAllTasks method
	private <T> List<Future<T>> runAllTasks(Collection<Callable<T>> tasks, ConcurrentLinkedQueue<Exception> errorQueue,
			ExecutorService executor) throws InterruptedException {
		// Your existing implementation
		return executor.invokeAll(tasks);
	}

	private String getErrorFilePath(String originalPath) {
		Path path = Paths.get(originalPath);
		String filename = path.getFileName().toString();

		// Replace .json with _Error.json
		String newFilename = filename.replace(".json", "_Error.json");

		return path.getParent().resolve(newFilename).toString();
	}

}
