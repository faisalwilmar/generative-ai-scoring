package org.ui.thesis.clients.deepseek;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.ui.thesis.clients.deepseek.models.DeepSeekMessage;
import org.ui.thesis.clients.deepseek.models.DeepSeekModel;
import org.ui.thesis.clients.deepseek.models.DeepSeekNoStreamResponse;
import org.ui.thesis.clients.deepseek.models.DeepSeekRequest;
import org.ui.thesis.clients.deepseek.models.JsonProperty;
import org.ui.thesis.exceptions.DeepSeekException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DeepSeekClientImpl implements DeepSeekClient {

	private final Duration TIMEOUT = Duration.ofSeconds(30);

	private final HttpClient httpClient;

	private final ObjectMapper objectMapper;

	private final String apiKey;

	private final String baseUrl;

	public DeepSeekClientImpl(String apiKey, String baseUrl) {
		this.apiKey = apiKey;
		this.baseUrl = baseUrl;
		this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
		this.objectMapper = new ObjectMapper();
		objectMapper.registerModule(new Jdk8Module());
	}

	/**
	 * Client using {@code https://api.deepseek.com/v1} as default baseUrl.
	 * @param apiKey your API Key.
	 */
	public DeepSeekClientImpl(String apiKey) {
		this(apiKey, "https://api.deepseek.com/v1");
	}

	/**
	 * Default Client using environment variable {@code DEEPSEEK_API_KEY} for API Key.
	 */
	public DeepSeekClientImpl() {
		this(System.getenv("DEEPSEEK_API_KEY"));
	}

	@Override
	public void healthCheck() {
		try {
			DeepSeekNoStreamResponse response = this.chat(DeepSeekModel.DEEPSEEK_CHAT,
					new ArrayList<>(List.of(DeepSeekMessage.ofUser("Explain how AI works in a few words"))));
			log.info("RESULT: " + response.getChoices().getFirst().getMessage().getContent());
			log.info("TOKEN USAGE: " + response.getUsage().getTotalTokens());
			String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
			log.warn(jsonResponse);
		}
		catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	/**
	 * Response Content is a text.
	 */
	@Override
	public DeepSeekNoStreamResponse chat(DeepSeekModel model, ArrayList<DeepSeekMessage> messages)
			throws DeepSeekException {
		return chat(model, messages, null, null, null, new DeepSeekRequest.ResponseFormat("text"), null);
	}

	@Override
	public <T> Pair<Integer, T> responseJson(Class<T> type, DeepSeekModel model, ArrayList<DeepSeekMessage> messages,
			List<JsonProperty> properties, Double temperature) {
		return responseJson(type, model, messages, properties, temperature, false);
	}

	@Override
	public <T> Pair<Integer, T> responseJson(Class<T> type, DeepSeekModel model, ArrayList<DeepSeekMessage> messages,
			List<JsonProperty> properties, Double temperature, boolean logRawResult) {

		try {
			DeepSeekNoStreamResponse response = chatJson(model, messages, properties, temperature);
			Integer tokenUsage = response.getUsage().getTotalTokens();

			T result = objectMapper.readValue(response.getChoices().getFirst().getMessage().getContent(), type);

			if (logRawResult) {
				String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
				log.info("Actual Model Response: {}", jsonResponse);
			}

			return Pair.of(tokenUsage, result);
		}
		catch (Exception e) {
			log.error(e.getMessage());

			return Pair.of(0, null);
		}

	}

	/**
	 * Response Content is a parseable json format.
	 */
	@Override
	public DeepSeekNoStreamResponse chatJson(DeepSeekModel model, ArrayList<DeepSeekMessage> messages,
			List<JsonProperty> properties, Double temperature) throws DeepSeekException {

		StringBuilder promptBuilder = new StringBuilder();
		promptBuilder.append("Response in JSON format with properties: ");

		for (int i = 0; i < properties.size(); i++) {
			JsonProperty prop = properties.get(i);
			String typeName = prop.dataType().getSimpleName().toLowerCase();
			promptBuilder.append(prop.name()).append("(").append(typeName).append(")");
			if (i < properties.size() - 1) {
				promptBuilder.append(", ");
			}
		}
		promptBuilder.append(". Make it compact and directly parseable to Java Object.");
		String jsonPrompt = promptBuilder.toString();

		messages.add(DeepSeekMessage.ofSystem(jsonPrompt));

		return chat(model, messages, null, null, null, new DeepSeekRequest.ResponseFormat("json_object"), temperature);
	}

	@Override
	public DeepSeekNoStreamResponse chat(DeepSeekModel model, ArrayList<DeepSeekMessage> messages, Integer topP,
			Integer frequencyPenalty, Integer maxTokens, DeepSeekRequest.ResponseFormat responseFormat,
			Double temperature) throws DeepSeekException {
		DeepSeekRequest request = DeepSeekRequest.builder()
			.messages(messages)
			.model(model.getModelName())
			.topP(topP)
			.frequencyPenalty(frequencyPenalty)
			.maxTokens(maxTokens)
			.responseFormat(responseFormat)
			.temperature(temperature)
			.stream(false)
			.build();

		try {
			HttpRequest httpRequest = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + "/chat/completions"))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + apiKey)
				.timeout(TIMEOUT)
				.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
				.build();

			HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				throw new DeepSeekException(
						"API request failed with status code: " + response.statusCode() + ", body: " + response.body());
			}

			return objectMapper.readValue(response.body(), DeepSeekNoStreamResponse.class);

		}
		catch (IOException e) {
			throw new DeepSeekException("Failed to process request/response", e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DeepSeekException("Request was interrupted", e);
		}
		catch (Exception e) {
			throw new DeepSeekException("Something went wrong", e);
		}
	}

}
