package org.ui.thesis.clients.deepseek.models;

import lombok.Getter;

@Getter
public enum DeepSeekModel {

	DEEPSEEK_CHAT("deepseek-chat"), DEEPSEEK_REASONER("deepseek-reasoner");

	private final String modelName;

	DeepSeekModel(String modelName) {
		this.modelName = modelName;
	}

}
