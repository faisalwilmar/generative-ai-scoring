package org.ui.thesis.services.studentscoring.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.ui.thesis.enums.QuestionType;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnswerScoreDto {

    public String semester;

    public String faculty;

    public String level;

    public String fullName;

    public String studentId;

    public String email;

    public QuestionType questionType;

    public String answer;

    public int finalScore;

    public int originalScore;

}
