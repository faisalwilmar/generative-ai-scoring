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
import org.ui.thesis.utils.CommonUtil;
import org.ui.thesis.utils.JsonFileUtil;
import org.ui.thesis.utils.StopWatchUtil;
import org.ui.thesis.utils.TextFileUtil;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

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
		log.info("=".repeat(80));
		log.info("Starting initial execution for {} students", answerScoreQuestion3List.size());
		log.info("AI Model: {}, Technique: {}", aiModel, promptTechnique);
		log.info("Max Retry Attempts: {}, Retry Wait: {}s", maxRetryAttempts, retryWaitSeconds);
		log.info("=".repeat(80));

		List<AnswerScoreDto> remainingTasks = new ArrayList<>(answerScoreQuestion3List);
		int attemptNumber = 1;

		while (!remainingTasks.isEmpty() && attemptNumber <= maxRetryAttempts + 1) {
			if (attemptNumber > 1) {
				log.info("");
				log.info("=".repeat(80));
				log.info("RETRY ATTEMPT {} of {}", attemptNumber - 1, maxRetryAttempts);
				log.info("Retrying {} failed tasks", remainingTasks.size());
				log.info("Waiting {}s before retry...", retryWaitSeconds);
				log.info("=".repeat(80));

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
			ExecutionResult result = executeTasksSync(remainingTasks, exampleShot, scoringGuide, aiModel,
					promptTechnique, errorQueue, feedbackOutputFilePath);

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
	 * Execute tasks for a list of students (Synchronous version with periodic saves)
	 */
	private ExecutionResult executeTasksSync(List<AnswerScoreDto> students, List<AnswerScoreDto> exampleShot,
			String scoringGuide, AiModel aiModel, PromptTechnique promptTechnique,
			ConcurrentLinkedQueue<Exception> errorQueue, String feedbackOutputFilePath) {

		List<AiScoringResult> successfulResults = new ArrayList<>();
		List<AnswerScoreDto> failedStudents = new ArrayList<>();

		// For incremental saves
		int saveInterval = 10;
		int batchNumber = 1;

		int index = 0;
		for (AnswerScoreDto answerScoreDto : students) {
			index++;

			// Execute task immediately and get result
			TaskResult taskResult = executeTaskSync(answerScoreDto, exampleShot, scoringGuide, aiModel, promptTechnique,
					errorQueue, index);

			// Process result immediately
			if (taskResult.success()) {
				successfulResults.add(taskResult.scoringResult());
			}
			else {
				failedStudents.add(taskResult.originalDto());
			}

			// Save every 10 executions
			if (index % saveInterval == 0) {
				String tempFilePath = getTempFilePath(feedbackOutputFilePath, batchNumber);
				JsonFileUtil.writeObjectToFile(successfulResults, tempFilePath);
				log.info("Checkpoint saved: {} students processed → {}", index, tempFilePath);
				CommonUtil.beep(2, 1000); // Double beep for save
				batchNumber++;
			}
		}

		// Save any remaining results that didn't reach the interval
		if (index % saveInterval != 0) {
			String tempFilePath = getTempFilePath(feedbackOutputFilePath, batchNumber);
			JsonFileUtil.writeObjectToFile(successfulResults, tempFilePath);
			log.info("Final checkpoint saved: {} students processed → {}", index, tempFilePath);
		}

		return new ExecutionResult(successfulResults, failedStudents);
	}

	/**
	 * Generate temporary file path with prefix Example: "output/results.json" →
	 * "output/temp_1_results.json"
	 */
	private String getTempFilePath(String originalPath, int batchNumber) {
		File file = new File(originalPath);
		String directory = file.getParent();
		String fileName = file.getName();

		String tempFileName = String.format("temp_%d_%s", batchNumber, fileName);

		if (directory != null) {
			return directory + File.separator + tempFileName;
		}
		else {
			return tempFileName;
		}
	}

	/**
	 * Execute a single scoring task (same as before)
	 */
	private TaskResult executeTaskSync(AnswerScoreDto answerScoreDto, List<AnswerScoreDto> exampleShot,
			String scoringGuide, AiModel aiModel, PromptTechnique promptTechnique,
			ConcurrentLinkedQueue<Exception> errorQueue, int index) {

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
			log.info("[{}][Processed {} students]", answerScoreDto.getStudentId(), index);
			CommonUtil.beep(1);

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
			CommonUtil.beep(5, 1000);
			return TaskResult.failure(answerScoreDto);
		}
	}

	/**
	 * Execute tasks for a list of students
	 */
	private ExecutionResult executeTasksAsync(List<AnswerScoreDto> students, List<AnswerScoreDto> exampleShot,
			String scoringGuide, AiModel aiModel, PromptTechnique promptTechnique,
			ConcurrentLinkedQueue<Exception> errorQueue) {

		Collection<Callable<TaskResult>> tasks = new ArrayList<>();

		int index = 0;
		for (AnswerScoreDto answerScoreDto : students) {
			index++;
			final int currentIndex = index; // Create final copy
			tasks.add(() -> executeTaskAsync(answerScoreDto, exampleShot, scoringGuide, aiModel, promptTechnique,
					errorQueue, currentIndex));
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
	private TaskResult executeTaskAsync(AnswerScoreDto answerScoreDto, List<AnswerScoreDto> exampleShot,
			String scoringGuide, AiModel aiModel, PromptTechnique promptTechnique,
			ConcurrentLinkedQueue<Exception> errorQueue, int index) {

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
			log.info("[{}][Processed {} students]", answerScoreDto.getStudentId(), index);
			CommonUtil.beep(1);

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
			CommonUtil.beep(5, 1000);
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
		log.info("=".repeat(80));
		log.info("FINAL EXECUTION SUMMARY");
		log.info("=".repeat(80));
		log.info("Total Tasks: {}", totalTasks);
		log.info("Successful: {}", successCount);
		log.info("Failed: {}", failedCount);
		log.info("Success Rate: {}", String.format("%.2f", (successCount * 100.0 / totalTasks)));
		log.info("=".repeat(80));

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
		String newFilename = filename.replace(".json", "_Error.txt");

		return path.getParent().resolve(newFilename).toString();
	}

	/**
	 * Manual recovery function to complete failed/missing student scores
	 * @param originalScoreFilePath Path to the original student scores file
	 * @param tempResultFilePath Path to the temporary results file to recover from
	 * @param feedbackOutputFilePath Path where the completed results should be saved
	 * @param aiModel The AI model to use for scoring
	 * @param promptTechnique The prompt technique to use
	 * @param maxRetryAttempts Maximum retry attempts for failed tasks
	 * @param retryWaitSeconds Wait time between retries
	 */
	public void executeQuestion3ManualRecovery(String originalScoreFilePath, String tempResultFilePath,
			String feedbackOutputFilePath, AiModel aiModel, PromptTechnique promptTechnique, int maxRetryAttempts,
			int retryWaitSeconds) {

		QuestionType questionType = QuestionType.QUESTION_3;

		log.info("=".repeat(80));
		log.info("STARTING MANUAL RECOVERY");
		log.info("=".repeat(80));
		log.info("Original scores: {}", originalScoreFilePath);
		log.info("Temp results: {}", tempResultFilePath);
		log.info("Output path: {}", feedbackOutputFilePath);
		log.info("AI Model: {}, Technique: {}", aiModel, promptTechnique);
		log.info("=".repeat(80));
		CommonUtil.beep(3, 2000); // Alert user that recovery is starting

		// Step 1: Load original student scores
		log.info("Step 1: Loading original student scores...");
		Map<String, List<AnswerScoreDto>> processedRecords = JsonFileUtil.readJsonByReference(originalScoreFilePath,
				new TypeReference<>() {
				});

		List<AnswerScoreDto> allOriginalScores = Objects.requireNonNull(processedRecords)
			.values()
			.stream()
			.filter(Objects::nonNull)
			.flatMap(List::stream)
			.filter(dto -> questionType.equals(dto.getQuestionType()))
			.toList();

		log.info("Loaded {} original student scores for {}", allOriginalScores.size(), questionType.name());

		// Step 2: Load existing temp results
		log.info("Step 2: Loading temp results...");
		List<AiScoringResult> existingResults = JsonFileUtil.readJsonArrayFromFile(tempResultFilePath,
				AiScoringResult.class);

		if (existingResults == null || existingResults.isEmpty()) {
			log.warn("No existing results found in temp file. Operation Cancelled.");
			return;
		}
		else {
			log.info("Loaded {} existing results from temp file", existingResults.size());
		}

		// Step 3: Identify missing students
		log.info("Step 3: Identifying missing students...");
		Set<String> completedStudentIds = existingResults.stream()
			.map(AiScoringResult::getStudentId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		List<AnswerScoreDto> missingStudents = allOriginalScores.stream()
			.filter(dto -> !completedStudentIds.contains(dto.getStudentId()))
			.toList();

		log.info("=".repeat(80));
		log.info("RECOVERY ANALYSIS:");
		log.info("  Total students: {}", allOriginalScores.size());
		log.info("  Already completed: {}", completedStudentIds.size());
		log.info("  Missing/Failed: {}", missingStudents.size());
		log.info("=".repeat(80));

		if (missingStudents.isEmpty()) {
			log.info("All students already processed! Nothing to recover.");
			log.info("Copying temp file to output...");
			JsonFileUtil.writeObjectToFile(existingResults, feedbackOutputFilePath);
			CommonUtil.beep(3, 2000);
			return;
		}

		// Step 4: Show details of missing students
		log.info("Missing students details:");
		for (int i = 0; i < Math.min(10, missingStudents.size()); i++) {
			AnswerScoreDto dto = missingStudents.get(i);
			log.info("  {}. {} - {} - {}", i + 1, dto.getStudentId(), dto.getFullName(), dto.getLevel());
		}
		if (missingStudents.size() > 10) {
			log.info("  ... and {} more", missingStudents.size() - 10);
		}

		// Step 5: Load example shots and scoring guide
		log.info("Step 4: Loading scoring resources...");
		List<AnswerScoreDto> exampleShot = loadExampleShots(promptTechnique);
		String scoringGuide = TextFileUtil.readAllText(INPUT_QUESTION_3_SCORING_GUIDE_FILE_PATH);
		log.info("Resources loaded");

		// Step 6: Process missing students with retry logic
		log.info("=".repeat(80));
		log.info("Step 5: Processing missing students...");
		log.info("=".repeat(80));
		CommonUtil.beep(2);

		ConcurrentLinkedQueue<Exception> errorQueue = new ConcurrentLinkedQueue<>();
		List<AiScoringResult> newResults = new ArrayList<>(existingResults);
		List<AnswerScoreDto> remainingTasks = new ArrayList<>(missingStudents);
		int attemptNumber = 1;

		while (!remainingTasks.isEmpty() && attemptNumber <= maxRetryAttempts + 1) {
			if (attemptNumber > 1) {
				log.info("");
				log.info("=".repeat(80));
				log.info("RECOVERY RETRY ATTEMPT {} of {}", attemptNumber - 1, maxRetryAttempts);
				log.info("Retrying {} failed tasks", remainingTasks.size());
				log.info("Waiting {}s before retry...", retryWaitSeconds);
				log.info("=".repeat(80));
				CommonUtil.beep(3, 2000);

				try {
					Thread.sleep(retryWaitSeconds * 1000L);
				}
				catch (InterruptedException e) {
					log.error("Retry wait interrupted");
					Thread.currentThread().interrupt();
					break;
				}
			}

			errorQueue.clear();

			// Create recovery output path for this attempt
			String recoveryTempPath = getRecoveryTempFilePath(feedbackOutputFilePath, attemptNumber);

			ExecutionResult result = executeTasksSync(remainingTasks, exampleShot, scoringGuide, aiModel,
					promptTechnique, errorQueue, recoveryTempPath);

			// Add new successful results
			newResults.addAll(result.successfulResults());
			remainingTasks = result.failedStudents();

			log.info("");
			log.info("-".repeat(80));
			log.info("Recovery Attempt {} Summary:", attemptNumber);
			log.info("  New successes: {}", result.successfulResults().size());
			log.info("  Still failed: {}", remainingTasks.size());
			log.info("  Total completed: {}", newResults.size());
			log.info("-".repeat(80));

			if (remainingTasks.isEmpty()) {
				log.info("All missing students recovered successfully!");
				CommonUtil.beep(3);
				break;
			}

			if (attemptNumber > maxRetryAttempts) {
				log.warn("Maximum retry attempts reached. {} students still failed.", remainingTasks.size());
				CommonUtil.beep(5);
				break;
			}

			attemptNumber++;
		}

		// Step 7: Save final results
		log.info("");
		log.info("=".repeat(80));
		log.info("RECOVERY COMPLETE - Saving final results...");
		log.info("=".repeat(80));

		JsonFileUtil.writeObjectToFile(newResults, feedbackOutputFilePath);

		// Step 8: Final summary
		log.info("");
		log.info("=".repeat(80));
		log.info("MANUAL RECOVERY SUMMARY");
		log.info("=".repeat(80));
		log.info("Original total: {}", allOriginalScores.size());
		log.info("Previously completed: {}", existingResults.size());
		log.info("Newly recovered: {}", newResults.size() - existingResults.size());
		log.info("Final total: {}", newResults.size());
		log.info("Still missing: {}", remainingTasks.size());
		log.info("Success rate: {}", String.format("%.2f", (newResults.size() * 100.0 / allOriginalScores.size())));
		log.info("=".repeat(80));
		log.info("Output saved to: {}", feedbackOutputFilePath);

		if (!remainingTasks.isEmpty()) {
			StringBuilder failedIds = new StringBuilder();
			failedIds.append("Still failed after recovery:\n");
			for (AnswerScoreDto dto : remainingTasks) {
				failedIds.append(dto.getStudentId()).append("\n");
			}
			String errorFilePath = getErrorFilePath(feedbackOutputFilePath);
			TextFileUtil.writeAllText(failedIds.toString(), errorFilePath);
			log.error("Failed student IDs written to: {}", errorFilePath);
		}

		log.info("=".repeat(80));
		CommonUtil.beep(5);
	}

	/**
	 * Generate recovery temp file path Example: "output/results.json" →
	 * "output/recovery_attempt1_results.json"
	 */
	private String getRecoveryTempFilePath(String originalPath, int attemptNumber) {
		File file = new File(originalPath);
		String directory = file.getParent();
		String fileName = file.getName();

		String tempFileName = String.format("recovery_attempt%d_%s", attemptNumber, fileName);

		if (directory != null) {
			return directory + File.separator + tempFileName;
		}
		else {
			return tempFileName;
		}
	}

}
