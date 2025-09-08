package org.ui.thesis;

import com.google.genai.Client;
import org.ui.thesis.clients.deepseek.DeepSeekClient;
import org.ui.thesis.clients.deepseek.DeepSeekClientImpl;
import org.ui.thesis.clients.gemini.GeminiClient;
import org.ui.thesis.clients.gemini.GeminiClientImpl;
import org.ui.thesis.clients.openai.OpenAiClient;
import org.ui.thesis.clients.openai.OpenAiClientImpl;

public class Main {

	public static void main(String[] args) {
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