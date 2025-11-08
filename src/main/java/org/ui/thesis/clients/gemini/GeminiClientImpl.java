package org.ui.thesis.clients.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.ui.thesis.clients.gemini.models.GeminiModel;
import org.ui.thesis.dtos.ChatMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class GeminiClientImpl implements GeminiClient {

	private final Client googleClient;

	private final ObjectMapper objectMapper;

	public GeminiClientImpl(Client googleClient) {
		this.googleClient = googleClient;
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new Jdk8Module());
	}

	@Override
	public void healthCheck() {
		GenerateContentConfig contentConfig = GenerateContentConfig.builder().temperature(Float.valueOf("0")).build();

		GenerateContentResponse response = googleClient.models.generateContent("gemini-2.5-flash",
				"Explain how AI works in a few words", contentConfig);

		try {
			log.info("RESULT: " + response.text());
			log.info("TOKEN USAGE: " + response.usageMetadata().get().totalTokenCount().get());
			String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
			log.warn(jsonResponse);
		}
		catch (JsonProcessingException e) {
			log.error(e.getMessage());
		}
	}

	@Override
	public <T> Pair<Integer, T> response(Class<T> type, Float temperature, GeminiModel llmModel, List<ChatMessage> messages){
		try {
			GenerateContentConfig.Builder contentConfigBuilder = GenerateContentConfig.builder();

			Map<String, Schema> outputSchema = new HashMap<>();
			outputSchema.put("llm_grade", Schema.builder().type(Type.Known.INTEGER).description("The score you give").build());
			outputSchema.put("llm_feedback", Schema.builder().type(Type.Known.STRING).description("Feedback for student").build());

			Schema finalResponseSchema = Schema.builder()
					.properties(outputSchema)
					.type(Type.Known.OBJECT)
					.required(List.of("llm_grade", "llm_feedback"))
					.build();

			contentConfigBuilder.responseMimeType("application/json").responseSchema(finalResponseSchema);

			if (temperature != null && !temperature.isNaN() && !temperature.isInfinite())
				contentConfigBuilder.temperature(temperature);
			else contentConfigBuilder.temperature((float) 0);

			StringBuilder userPromptTextBuilder = new StringBuilder();

			StringBuilder systemPromptTextBuilder = new StringBuilder();

			for (ChatMessage chatMessage : messages){
				switch (chatMessage.role()) {
					case SYSTEM, ASSISTANT -> systemPromptTextBuilder.append(chatMessage.message());
                    default -> userPromptTextBuilder.append(chatMessage.message());
				}
			}

			String userPrompt = userPromptTextBuilder.toString().trim();
			String systemPrompt = systemPromptTextBuilder.toString().trim();

			if (!systemPrompt.isBlank())
				contentConfigBuilder.systemInstruction(
						Content.fromParts(Part.fromText(systemPrompt)));

			GenerateContentConfig contentConfig = contentConfigBuilder.build();

			GenerateContentResponse response = googleClient.models.generateContent(llmModel.getModelCode(),
					userPrompt, contentConfig);

			if (response.text() != null && !response.text().trim().isBlank()) {
				Integer tokenUsage = response.usageMetadata().get().totalTokenCount().get();
				T result = objectMapper.readValue(response.text(), type);

				String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
				log.info("Actual Model Response: {}", jsonResponse);

				return Pair.of(tokenUsage, result);
			}
			else {
				log.warn("Nothing Retrieved");

				return Pair.of(0, null);
			}
		}
		catch (Exception e) {
			log.error(e.getMessage());

			return Pair.of(0, null);
		}

	}

}
