package org.ui.thesis.enums;

import lombok.Getter;

@Getter
public enum UserRole {

	USER("user"), SYSTEM("system"), ASSISTANT("assistant");

	private final String role;

	UserRole(String role) {
		this.role = role;
	}

}
