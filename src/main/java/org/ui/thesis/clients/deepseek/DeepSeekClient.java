package org.ui.thesis.clients.deepseek;

import org.ui.thesis.clients.deepseek.models.DeepSeekMessage;
import org.ui.thesis.clients.deepseek.models.DeepSeekModel;
import org.ui.thesis.clients.deepseek.models.DeepSeekNoStreamResponse;
import org.ui.thesis.clients.deepseek.models.DeepSeekRequest;
import org.ui.thesis.exceptions.DeepSeekException;

import java.util.List;

public interface DeepSeekClient {

	DeepSeekNoStreamResponse chat(DeepSeekModel model, List<DeepSeekMessage> messages) throws DeepSeekException;

	DeepSeekNoStreamResponse chatJson(DeepSeekModel model, List<DeepSeekMessage> messages,
			List<DeepSeekClientImpl.JsonProperty> properties) throws DeepSeekException;

	DeepSeekNoStreamResponse chat(DeepSeekModel model, List<DeepSeekMessage> messages, Integer topP,
			Integer frequencyPenalty, Integer maxTokens, DeepSeekRequest.ResponseFormat responseFormat,
			Double temperature) throws DeepSeekException;

}
