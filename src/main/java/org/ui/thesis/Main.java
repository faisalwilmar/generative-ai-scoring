package org.ui.thesis;

import com.google.genai.Client;
import org.apache.commons.lang3.tuple.Pair;
import org.ui.thesis.clients.deepseek.DeepSeekClient;
import org.ui.thesis.clients.deepseek.DeepSeekClientImpl;
import org.ui.thesis.clients.gemini.GeminiClient;
import org.ui.thesis.clients.gemini.GeminiClientImpl;
import org.ui.thesis.clients.openai.OpenAiClient;
import org.ui.thesis.clients.openai.OpenAiClientImpl;
import org.ui.thesis.controllers.ScoringExecutor;
import org.ui.thesis.dtos.CompiledStudentScore;
import org.ui.thesis.enums.AiModel;
import org.ui.thesis.enums.PromptTechnique;
import org.ui.thesis.enums.QuestionType;
import org.ui.thesis.services.dataprocessor.DataFormatter;
import org.ui.thesis.services.dataprocessor.DataFormatterImpl;
import org.ui.thesis.services.studentscoring.StudentScoring;
import org.ui.thesis.services.studentscoring.StudentScoringImpl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Main {

	private static final String OUTPUT_FILE_NAME_PREFIX = "Result";

	private static final String INPUT_FILE_PATH = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Matched Responses to Grade.json";

	private static final String OUTPUT_QUESTION_3_AI_FEEDBACK_FILE_PATH_PREFIX = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Result/005 Experiment/";

	private static final String OUTPUT_COMPILED_FILE_PATH = OUTPUT_QUESTION_3_AI_FEEDBACK_FILE_PATH_PREFIX
			+ "compiled_scores.csv";

	private static final int DEFAULT_MAX_RETRY_ATTEMPTS = 3;

	private static final int DEFAULT_RETRY_WAIT_SECONDS = 5;

	public static void main(String[] args) {
		GeminiClient geminiClient = new GeminiClientImpl(Client.builder().build());
		DeepSeekClient deepSeekClient = new DeepSeekClientImpl();
		OpenAiClient openAiClient = new OpenAiClientImpl();

		StudentScoring studentScoring = new StudentScoringImpl(deepSeekClient, openAiClient, geminiClient);
		DataFormatter dataFormatter = new DataFormatterImpl();
		ScoringExecutor executor = new ScoringExecutor(studentScoring);

		List<Pair<AiModel, PromptTechnique>> scoringToExecutes = new ArrayList<>();
		scoringToExecutes.add(Pair.of(AiModel.CHATGPT, PromptTechnique.ZERO_SHOT));
		scoringToExecutes.add(Pair.of(AiModel.CHATGPT, PromptTechnique.FEW_SHOT));
		scoringToExecutes.add(Pair.of(AiModel.CHATGPT, PromptTechnique.CHAIN_OF_THOUGHT));
		scoringToExecutes.add(Pair.of(AiModel.GEMINI, PromptTechnique.ZERO_SHOT));
		scoringToExecutes.add(Pair.of(AiModel.GEMINI, PromptTechnique.FEW_SHOT));
		scoringToExecutes.add(Pair.of(AiModel.GEMINI, PromptTechnique.CHAIN_OF_THOUGHT));
		scoringToExecutes.add(Pair.of(AiModel.DEEPSEEK, PromptTechnique.ZERO_SHOT));
		scoringToExecutes.add(Pair.of(AiModel.DEEPSEEK, PromptTechnique.FEW_SHOT));
		scoringToExecutes.add(Pair.of(AiModel.DEEPSEEK, PromptTechnique.CHAIN_OF_THOUGHT));

		Map<Pair<AiModel, PromptTechnique>, String> aiResultFiles = new LinkedHashMap<>();
		aiResultFiles.put(Pair.of(AiModel.CHATGPT, PromptTechnique.ZERO_SHOT),
				OUTPUT_QUESTION_3_AI_FEEDBACK_FILE_PATH_PREFIX + OUTPUT_FILE_NAME_PREFIX + " ChatGPT Zero Shot.json");
		aiResultFiles.put(Pair.of(AiModel.CHATGPT, PromptTechnique.FEW_SHOT),
				OUTPUT_QUESTION_3_AI_FEEDBACK_FILE_PATH_PREFIX + OUTPUT_FILE_NAME_PREFIX + " ChatGPT Few Shot.json");
		aiResultFiles.put(Pair.of(AiModel.CHATGPT, PromptTechnique.CHAIN_OF_THOUGHT),
				OUTPUT_QUESTION_3_AI_FEEDBACK_FILE_PATH_PREFIX + OUTPUT_FILE_NAME_PREFIX + " ChatGPT CoT.json");
		aiResultFiles.put(Pair.of(AiModel.GEMINI, PromptTechnique.ZERO_SHOT),
				OUTPUT_QUESTION_3_AI_FEEDBACK_FILE_PATH_PREFIX + OUTPUT_FILE_NAME_PREFIX + " Gemini Zero Shot.json");
		aiResultFiles.put(Pair.of(AiModel.GEMINI, PromptTechnique.FEW_SHOT),
				OUTPUT_QUESTION_3_AI_FEEDBACK_FILE_PATH_PREFIX + OUTPUT_FILE_NAME_PREFIX + " Gemini Few Shot.json");
		aiResultFiles.put(Pair.of(AiModel.GEMINI, PromptTechnique.CHAIN_OF_THOUGHT),
				OUTPUT_QUESTION_3_AI_FEEDBACK_FILE_PATH_PREFIX + OUTPUT_FILE_NAME_PREFIX + " Gemini CoT.json");
		aiResultFiles.put(Pair.of(AiModel.DEEPSEEK, PromptTechnique.ZERO_SHOT),
				OUTPUT_QUESTION_3_AI_FEEDBACK_FILE_PATH_PREFIX + OUTPUT_FILE_NAME_PREFIX + " DeepSeek Zero Shot.json");
		aiResultFiles.put(Pair.of(AiModel.DEEPSEEK, PromptTechnique.FEW_SHOT),
				OUTPUT_QUESTION_3_AI_FEEDBACK_FILE_PATH_PREFIX + OUTPUT_FILE_NAME_PREFIX + " DeepSeek Few Shot.json");
		aiResultFiles.put(Pair.of(AiModel.DEEPSEEK, PromptTechnique.CHAIN_OF_THOUGHT),
				OUTPUT_QUESTION_3_AI_FEEDBACK_FILE_PATH_PREFIX + OUTPUT_FILE_NAME_PREFIX + " DeepSeek CoT.json");

		for (Pair<AiModel, PromptTechnique> scoringToExecute : scoringToExecutes) {
			// The Java equivalent of ConcurrentBag<Exception>
			ConcurrentLinkedQueue<Exception> sharedErrorQueue = new ConcurrentLinkedQueue<>();

			executor.ExecuteQuestion3ScoringWithRetry(sharedErrorQueue, INPUT_FILE_PATH,
					aiResultFiles.get(scoringToExecute), scoringToExecute.getLeft(), scoringToExecute.getRight(),
					DEFAULT_MAX_RETRY_ATTEMPTS, DEFAULT_RETRY_WAIT_SECONDS);
		}

		// === DO THIS AFTER ALL THE INPUT FINISHED

		// Compile data for QUESTION_3 (can be changed to other question types)
		List<CompiledStudentScore> compiledScores = dataFormatter.compileDataForQuestionType(INPUT_FILE_PATH,
				aiResultFiles, QuestionType.QUESTION_3);

		// Export to CSV
		dataFormatter.exportToCSV(compiledScores, OUTPUT_COMPILED_FILE_PATH);
	}

	private void aiClientHealthCheck() {
		GeminiClient geminiClient = new GeminiClientImpl(Client.builder().build());
		System.out.println("Gemini Result:");
		geminiClient.healthCheck();

		DeepSeekClient deepSeekClient = new DeepSeekClientImpl();
		System.out.println("DeepSeek Result:");
		deepSeekClient.healthCheck();

		OpenAiClient openAiClient = new OpenAiClientImpl();
		System.out.println("OpenAi Result:");
		openAiClient.healthCheck();
	}

}