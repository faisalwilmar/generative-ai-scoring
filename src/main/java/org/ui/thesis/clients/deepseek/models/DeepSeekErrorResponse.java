package org.ui.thesis.clients.deepseek.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class DeepSeekErrorResponse {

	private ErrorDetails error;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ErrorDetails {

		private String message;

		private String type;

		private String param;

		private String code;

	}

}
