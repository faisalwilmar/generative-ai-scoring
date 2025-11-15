package org.ui.thesis.clients.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputText;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.ui.thesis.dtos.ChatMessage;

import java.util.List;
import java.util.Optional;

@Slf4j
public class OpenAiClientImpl implements OpenAiClient {

	private final OpenAIClient client;

	private final ObjectMapper objectMapper;

	public OpenAiClientImpl() {
		this.client = OpenAIOkHttpClient.fromEnv();
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new Jdk8Module());
	}

	@Override
	public void healthCheck() {
		try {
			ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
				.addUserMessage("Explain how AI works in a few words")
				.model(ChatModel.GPT_5_NANO)
				.build();
			ChatCompletion chatCompletion = client.chat().completions().create(params);
			ChatCompletion.Choice choice = chatCompletion.choices().getFirst();
			Optional<String> content = choice.message().content();
			if (content.isPresent()) {
				log.info("RESULT: " + content.get());
				log.info("TOKEN USAGE: " + chatCompletion.usage().get().totalTokens());
				String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(chatCompletion);
				log.warn(jsonResponse);
			}
			else {
				log.warn("Nothing Retrieved");
			}
		}
		catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	@Override
	public <T> Pair<Long, T> responseJson(Class<T> type, Double temperature, List<ChatMessage> messages,
			String promptCacheKey) {

		return responseJson(type, temperature, messages, ChatModel.GPT_4_1, promptCacheKey);
	}

	@Override
	public <T> Pair<Long, T> responseJson(Class<T> type, Double temperature, List<ChatMessage> messages,
			String promptCacheKey, boolean logRawResult) {

		return responseJson(type, temperature, messages, ChatModel.GPT_4_1, promptCacheKey, logRawResult);
	}

	@Override
	public <T> Pair<Long, T> responseJson(Class<T> type, Double temperature, List<ChatMessage> messages,
			ChatModel llmModel, String promptCacheKey) {
		return responseJson(type, temperature, messages, llmModel, promptCacheKey, false);
	}

	@Override
	public <T> Pair<Long, T> responseJson(Class<T> type, Double temperature, List<ChatMessage> messages,
			ChatModel llmModel, String promptCacheKey, boolean logRawResult) {

		try {
			ResponseCreateParams.Builder paramsBuilder = ResponseCreateParams.builder();

			if (temperature != null)
				paramsBuilder.temperature(temperature);

			if (promptCacheKey != null && !promptCacheKey.isBlank())
				paramsBuilder.promptCacheKey(promptCacheKey);

			StringBuilder inputBuilder = new StringBuilder();

			for (ChatMessage chatMessage : messages) {
				inputBuilder.append(chatMessage.message());
			}

			// paramsBuilder.reasoning(Reasoning.builder().effort(ReasoningEffort.HIGH).build());

			ResponseCreateParams params = paramsBuilder.input(inputBuilder.toString()).model(llmModel).build();
			Response response = client.responses().create(params);
			List<ResponseOutputItem> responseOutputItems = response.output();
			ResponseOutputItem messageOutput = responseOutputItems.stream()
				.filter(ResponseOutputItem::isMessage)
				.toList()
				.getFirst();

			if (messageOutput != null) {
				ResponseOutputText responseOutputText = messageOutput.message()
					.get()
					.content()
					.getFirst()
					.asOutputText();
				Long tokenUsage = response.usage().get().totalTokens();
				T result = objectMapper.readValue(responseOutputText.text(), type);

				if (logRawResult) {
					String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
					log.info("Actual Model Response: {}", jsonResponse);
				}

				return Pair.of(tokenUsage, result);
			}
			else {
				log.warn("Nothing Retrieved");

				return Pair.of(Integer.toUnsignedLong(0), null);
			}
		}
		catch (Exception e) {
			log.error(e.getMessage());

			return Pair.of(Integer.toUnsignedLong(0), null);
		}

	}

	@Override
	public <T> Pair<Long, T> chat(Class<T> type, Double temperature, List<ChatMessage> messages) {
		try {
			ChatCompletionCreateParams.Builder paramsBuilder = ChatCompletionCreateParams.builder();

			paramsBuilder.temperature(temperature);

			for (ChatMessage chatMessage : messages) {
				switch (chatMessage.role()) {
					case SYSTEM -> paramsBuilder.addSystemMessage(chatMessage.message());
					case ASSISTANT -> paramsBuilder.addAssistantMessage(chatMessage.message());
					default -> paramsBuilder.addUserMessage(chatMessage.message());
				}
			}

			paramsBuilder.model(ChatModel.GPT_5_NANO);
			ChatCompletionCreateParams contentParam = paramsBuilder.build();

			ChatCompletion chatCompletion = client.chat().completions().create(contentParam);
			ChatCompletion.Choice choice = chatCompletion.choices().getFirst();
			Optional<String> content = choice.message().content();
			if (content.isPresent()) {
				long tokenUsage = chatCompletion.usage().get().totalTokens();
				T result = objectMapper.readValue(content.get(), type);

				String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(chatCompletion);
				log.info("Actual Model Response: {}", jsonResponse);

				return Pair.of(tokenUsage, result);
			}
			else {
				log.warn("Nothing Retrieved");

				return Pair.of(Integer.toUnsignedLong(0), null);
			}
		}
		catch (Exception e) {
			log.error(e.getMessage());

			return Pair.of(Integer.toUnsignedLong(0), null);
		}
	}

}
