package org.ui.thesis.clients.deepseek.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeepSeekUsage {

	private int completionTokens;

	private int promptTokens;

	private int promptCacheHitTokens;

	private int promptCacheMissTokens;

	private int totalTokens;

}
