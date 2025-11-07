package org.ui.thesis.clients.gemini;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum GeminiModel {

    GEMINI_2_5_PRO("gemini-2.5-pro"), GEMINI_2_5_FLASH("gemini-2.5-flash"),
    GEMINI_2_5_FLASH_LITE("gemini-2.5-flash-lite");

    private final String modelCode;
}
