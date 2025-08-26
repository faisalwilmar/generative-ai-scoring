package org.ui.thesis.client.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GeminiClientImpl implements GeminiClient {

	private final Client googleClient;

	@Override
	public void healthCheck() {
		GenerateContentResponse response = googleClient.models.generateContent("gemini-2.5-flash",
				"Explain how AI works in a few words",
				GenerateContentConfig.builder().temperature(Float.valueOf("0")).build());

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new Jdk8Module());

		try {
			log.info("HASILNYA:" + response.text());
			log.info("TOKEN USAGE:" + response.usageMetadata().get().totalTokenCount());
			String jsonResponse = objectMapper.writeValueAsString(response);
			log.warn(jsonResponse);
		}
		catch (JsonProcessingException e) {
			log.error(e.getMessage());
		}
	}

}
