package org.ui.thesis.dtos;

import lombok.Getter;
import org.ui.thesis.enums.UserRole;

/**
 * @param role
 * @param message The message.
 */
public record ChatMessage(UserRole role, String message) {
}
