package org.ui.thesis.clients.openai;

import com.openai.models.ChatModel;
import org.apache.commons.lang3.tuple.Pair;
import org.ui.thesis.dtos.ChatMessage;

import java.util.List;

public interface OpenAiClient {

	void healthCheck();

	/**
	 * By default using gpt-4.1.
	 * @param type Output Object Class.
	 * @param temperature between 0 and 2. Higher values like 0.8 will make the output
	 * more random, while lower values like 0.2 will make it more focused and
	 * deterministic.
	 * @param messages
	 * @param promptCacheKey Used by OpenAI to cache responses for similar requests to
	 * optimize your cache hit rates. Automatically Cache input token if above 1000.
	 * @return Formatted Object.
	 * @param <T>
	 */
	<T> Pair<Long, T> responseJson(Class<T> type, Double temperature, List<ChatMessage> messages,
			String promptCacheKey);

	/**
	 * By default using gpt-4.1.
	 * @param type Output Object Class.
	 * @param temperature between 0 and 2. Higher values like 0.8 will make the output
	 * more random, while lower values like 0.2 will make it more focused and
	 * deterministic. Should be {@code null} when using Reasoning Model (gpt-5 variant).
	 * @param messages
	 * @param llmModel LLM Model to use. Reasoning model (all gpt-5 variant), will use
	 * High Reasoning Effort to ensure best result.
	 * @param promptCacheKey Used by OpenAI to cache responses for similar requests to
	 * optimize your cache hit rates. Automatically Cache input token if above 1000.
	 * @return Formatted Object.
	 * @param <T>
	 */
	<T> Pair<Long, T> responseJson(Class<T> type, Double temperature, List<ChatMessage> messages, ChatModel llmModel,
			String promptCacheKey);

	@Deprecated
	<T> Pair<Long, T> chat(Class<T> type, Double temperature, List<ChatMessage> messages);

}
