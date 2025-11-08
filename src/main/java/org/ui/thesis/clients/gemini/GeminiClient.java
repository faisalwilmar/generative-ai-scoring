package org.ui.thesis.clients.gemini;

import org.apache.commons.lang3.tuple.Pair;
import org.ui.thesis.clients.gemini.models.GeminiModel;
import org.ui.thesis.dtos.ChatMessage;

import java.util.List;

public interface GeminiClient {

	void healthCheck();

    <T> Pair<Integer, T> response(Class<T> type, Float temperature, GeminiModel llmModel, List<ChatMessage> messages);
}
