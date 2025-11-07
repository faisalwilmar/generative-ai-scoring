package org.ui.thesis.clients.deepseek.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ui.thesis.enums.UserRole;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeepSeekMessage {

	/**
	 * Actual content (that you usually type in the web portal for chatting with the AI)
	 */
	private String content;

	/**
	 * @see UserRole for possible value.
	 */
	private String role;

	public static DeepSeekMessage ofUser(String messageContent) {
		return new DeepSeekMessage(messageContent, UserRole.USER.getRole());
	}

	public static DeepSeekMessage ofSystem(String messageContent) {
		return new DeepSeekMessage(messageContent, UserRole.SYSTEM.getRole());
	}

}
