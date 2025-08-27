package org.ui.thesis.clients.deepseek.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeepSeekRequest {

	private final List<DeepSeekMessage> messages;

	private final String model;

	@JsonProperty("frequency_penalty")
	private Integer frequencyPenalty;

	@JsonProperty("max_tokens")
	private Integer maxTokens;

	@JsonProperty("response_format")
	private ResponseFormat responseFormat;

	@Builder.Default
	private boolean stream = false;

	private Double temperature;

	@JsonProperty("top_p")
	private Integer topP;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ResponseFormat {

		/**
		 * @implSpec possible values are {@code text} or {@code json_object}. When you
		 * choose {@code json_object} then you must specify in the message that the
		 * response should be in JSON format.
		 */
		private String type;

	}

}
