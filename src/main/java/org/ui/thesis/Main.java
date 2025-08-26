package org.ui.thesis;

import com.google.genai.Client;
import org.ui.thesis.client.gemini.GeminiClient;
import org.ui.thesis.client.gemini.GeminiClientImpl;

public class Main {

	public static void main(String[] args) {
		GeminiClient geminiClient = new GeminiClientImpl(Client.builder().build());

		geminiClient.healthCheck();
	}

}