package org.ui.thesis.clients.deepseek.models;

/**
 * @param name
 * @param description Data description,
 * @param dataType Data Type class, e.g: {@code int.class}, {@code String.class},
 * {@code Integer.class}.
 */
public record JsonProperty(String name, String description, Class<?> dataType) {
}
