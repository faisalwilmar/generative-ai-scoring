package org.ui.thesis.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ui.thesis.enums.AiModel;
import org.ui.thesis.enums.PromptTechnique;
import org.ui.thesis.enums.QuestionType;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiScoringResult {

    private String semester;

    private String faculty;

    private String level;

    private String fullName;

    private String studentId;

    private QuestionType questionType;

    private String answer;

    private AiModel aiModel;

    private PromptTechnique promptTechnique;

    private int aiScore;

    private String aiFeedback;

    private Long tokenUsage;
}
