package org.ui.thesis.exceptions;

import lombok.Getter;

@Getter
public class ConcurrentException extends Exception {

	private String operationId;

	public ConcurrentException(String operationId, String message) {
		super(message);
		this.operationId = operationId;
	}

	public ConcurrentException(String message) {
		super(message);
	}

	public ConcurrentException(String operationId, String message, Throwable cause) {
		super(message, cause);
		this.operationId = operationId;
	}

	public ConcurrentException(String message, Throwable cause) {
		super(message, cause);
	}

}
