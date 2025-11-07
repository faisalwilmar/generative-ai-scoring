package org.ui.thesis.services.studentscoring.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiScoringFeedbackDto {

	@JsonSetter(nulls = Nulls.AS_EMPTY)
	public int llm_grade;

	@JsonSetter(nulls = Nulls.AS_EMPTY)
	public String llm_feedback;

}
