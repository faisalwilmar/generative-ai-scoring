package org.ui.thesis.clients.deepseek;

import org.apache.commons.lang3.tuple.Pair;
import org.ui.thesis.clients.deepseek.models.DeepSeekMessage;
import org.ui.thesis.clients.deepseek.models.DeepSeekModel;
import org.ui.thesis.clients.deepseek.models.DeepSeekNoStreamResponse;
import org.ui.thesis.clients.deepseek.models.DeepSeekRequest;
import org.ui.thesis.clients.deepseek.models.JsonProperty;
import org.ui.thesis.exceptions.DeepSeekException;

import java.util.ArrayList;
import java.util.List;

public interface DeepSeekClient {

	void healthCheck();

	DeepSeekNoStreamResponse chat(DeepSeekModel model, ArrayList<DeepSeekMessage> messages) throws DeepSeekException;

	<T> Pair<Integer, T> responseJson(Class<T> type, DeepSeekModel model, ArrayList<DeepSeekMessage> messages,
			List<JsonProperty> properties, Double temperature);

	DeepSeekNoStreamResponse chatJson(DeepSeekModel model, ArrayList<DeepSeekMessage> messages,
			List<JsonProperty> properties, Double temperature) throws DeepSeekException;

	DeepSeekNoStreamResponse chat(DeepSeekModel model, ArrayList<DeepSeekMessage> messages, Integer topP,
			Integer frequencyPenalty, Integer maxTokens, DeepSeekRequest.ResponseFormat responseFormat,
			Double temperature) throws DeepSeekException;

}
