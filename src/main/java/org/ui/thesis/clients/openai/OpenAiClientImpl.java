package org.ui.thesis.clients.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import lombok.extern.slf4j.Slf4j;

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

}
