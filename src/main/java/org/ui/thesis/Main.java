package org.ui.thesis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.genai.Client;
import org.ui.thesis.clients.deepseek.DeepSeekClient;
import org.ui.thesis.clients.deepseek.DeepSeekClientImpl;
import org.ui.thesis.clients.gemini.GeminiClient;
import org.ui.thesis.clients.gemini.GeminiClientImpl;
import org.ui.thesis.clients.openai.OpenAiClient;
import org.ui.thesis.clients.openai.OpenAiClientImpl;
import org.ui.thesis.controllers.ScoringExecutor;
import org.ui.thesis.enums.AiModel;
import org.ui.thesis.enums.PromptTechnique;
import org.ui.thesis.services.dataprocessor.DataFormatter;
import org.ui.thesis.services.dataprocessor.DataFormatterImpl;
import org.ui.thesis.services.studentscoring.StudentScoring;
import org.ui.thesis.services.studentscoring.StudentScoringImpl;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Main {

	private static final String INPUT_FILE_PATH_ZERO_SHOT = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Result/Zero Shot Sample/Zero Shot Sample to Process.json";

	private static final String INPUT_FILE_PATH_FEW_SHOT = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Result/Few Shot Sample/Few Shot Sample to Process.json";

	private static final String OUTPUT_QUESTION_3_AI_FEEDBACK_FILE_PATH = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Result/Sample Result.json";

	private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());

	public static void main(String[] args) {
		GeminiClient geminiClient = new GeminiClientImpl(Client.builder().build());
		DeepSeekClient deepSeekClient = new DeepSeekClientImpl();
		OpenAiClient openAiClient = new OpenAiClientImpl();

		StudentScoring studentScoring = new StudentScoringImpl(deepSeekClient, openAiClient, geminiClient);
		DataFormatter dataFormatter = new DataFormatterImpl();
		ScoringExecutor executor = new ScoringExecutor(studentScoring, dataFormatter);

		ConcurrentLinkedQueue<Exception> sharedErrorQueue = new ConcurrentLinkedQueue<>();

		executor.ExecuteQuestion3Scoring(sharedErrorQueue, INPUT_FILE_PATH_FEW_SHOT,
				OUTPUT_QUESTION_3_AI_FEEDBACK_FILE_PATH, AiModel.GEMINI, PromptTechnique.FEW_SHOT);
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