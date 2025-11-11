package org.ui.thesis.services.studentscoring;

import com.openai.models.ChatModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.ui.thesis.clients.deepseek.DeepSeekClient;
import org.ui.thesis.clients.deepseek.models.DeepSeekMessage;
import org.ui.thesis.clients.deepseek.models.DeepSeekModel;
import org.ui.thesis.clients.deepseek.models.JsonProperty;
import org.ui.thesis.clients.gemini.GeminiClient;
import org.ui.thesis.clients.gemini.models.GeminiModel;
import org.ui.thesis.clients.openai.OpenAiClient;
import org.ui.thesis.constants.PromptConstant;
import org.ui.thesis.dtos.ChatMessage;
import org.ui.thesis.enums.AiModel;
import org.ui.thesis.enums.PromptTechnique;
import org.ui.thesis.enums.QuestionType;
import org.ui.thesis.enums.UserRole;
import org.ui.thesis.services.studentscoring.dto.AiScoringFeedbackDto;
import org.ui.thesis.services.studentscoring.dto.AnswerScoreDto;
import org.ui.thesis.services.studentscoring.dto.StudentAnswerDto;
import org.ui.thesis.services.studentscoring.dto.StudentGradeDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentScoringImpl implements StudentScoring {

	private DeepSeekClient deepSeekClient;

	private OpenAiClient openAiClient;

	private GeminiClient geminiClient;

	@Override
	public Pair<Integer, AiScoringFeedbackDto> getAiScoreAndFeedback(AiModel aiModel, PromptTechnique promptTechnique,
			List<AnswerScoreDto> exampleAnswerScore, String scoringGuide, String question, String answer) {

		Pair<Integer, AiScoringFeedbackDto> response = Pair.of(0, new AiScoringFeedbackDto());

		switch (aiModel) {
			case GEMINI -> {
				switch (promptTechnique) {
					case ZERO_SHOT -> {
						response = geminiZeroShotScoring(scoringGuide, question, answer);
					}
					case FEW_SHOT -> {
						response = geminiFewShotScoring(exampleAnswerScore, scoringGuide, question, answer);
					}
					case CHAIN_OF_THOUGHT -> {

					}
					default -> {
						response = null;
					}
				}
			}
			case CHATGPT -> {
				switch (promptTechnique) {
					case ZERO_SHOT -> {
						Pair<Long, AiScoringFeedbackDto> chatGptResponse = chatGptZeroShotScoring(scoringGuide,
								question, answer);
						response = Pair.of(chatGptResponse.getLeft().intValue(), chatGptResponse.getRight());
					}
					case FEW_SHOT -> {

					}
					case CHAIN_OF_THOUGHT -> {

					}
					default -> {
						response = null;
					}
				}
			}
			case DEEPSEEK -> {
				switch (promptTechnique) {
					case ZERO_SHOT -> {
						return deepSeekZeroShotScoring(scoringGuide, question, answer);
					}
					case FEW_SHOT -> {
						return deepSeekFewZeroShotScoring(exampleAnswerScore, scoringGuide, question, answer);
					}
					case CHAIN_OF_THOUGHT -> {

					}
					default -> {
						response = null;
					}
				}
			}
			default -> {
				response = null;
			}
		}

		return response;
	}

	private Pair<Long, AiScoringFeedbackDto> chatGptZeroShotScoring(String scoringGuide, String question,
			String answer) {
		String systemMessage = PromptConstant.InstructionSystemMessage.replace("{{scoring guide}}", scoringGuide)
			.replace("{{question}}", question);

		ChatMessage systemChatMessage = new ChatMessage(UserRole.SYSTEM, systemMessage);

		String userMessage = PromptConstant.ZeroShotUserMessage.replace("{{student answer}}", answer);

		ChatMessage userChatMessage = new ChatMessage(UserRole.USER, userMessage);

		// you may choose
		// return openAiClient.response(AiScoringFeedbackDto.class, 0.2,
		// List.of(systemChatMessage, userChatMessage), null);

		return openAiClient.responseJson(AiScoringFeedbackDto.class, null, List.of(systemChatMessage, userChatMessage),
				ChatModel.GPT_5, null);
	}

	private Pair<Integer, AiScoringFeedbackDto> geminiZeroShotScoring(String scoringGuide, String question,
			String answer) {
		String systemMessage = PromptConstant.InstructionSystemMessage.replace("{{scoring guide}}", scoringGuide)
			.replace("{{question}}", question);

		ChatMessage systemChatMessage = new ChatMessage(UserRole.SYSTEM, systemMessage);

		String userMessage = PromptConstant.ZeroShotUserMessage.replace("{{student answer}}", answer);

		ChatMessage userChatMessage = new ChatMessage(UserRole.USER, userMessage);

		return geminiClient.responseJson(AiScoringFeedbackDto.class, Float.valueOf("0.0"), GeminiModel.GEMINI_2_5_PRO,
				List.of(systemChatMessage, userChatMessage));
	}

	private Pair<Integer, AiScoringFeedbackDto> geminiFewShotScoring(List<AnswerScoreDto> exampleAnswerScore,
			String scoringGuide, String question, String answer) {
		String systemMessage = PromptConstant.InstructionSystemMessage.replace("{{scoring guide}}", scoringGuide)
			.replace("{{question}}", question);

		ChatMessage systemChatMessage = new ChatMessage(UserRole.SYSTEM, systemMessage);

		ChatMessage userChatMessage = getFewShotChatMessage(exampleAnswerScore, answer);

		return geminiClient.responseJson(AiScoringFeedbackDto.class, Float.valueOf("0.0"), GeminiModel.GEMINI_2_5_FLASH,
				List.of(systemChatMessage, userChatMessage));
	}

	private Pair<Integer, AiScoringFeedbackDto> deepSeekZeroShotScoring(String scoringGuide, String question,
			String answer) {
		String systemMessage = PromptConstant.InstructionSystemMessage.replace("{{scoring guide}}", scoringGuide)
			.replace("{{question}}", question);

		DeepSeekMessage systemChatMessage = DeepSeekMessage.ofSystem(systemMessage);

		String userMessage = PromptConstant.ZeroShotUserMessage.replace("{{student answer}}", answer);

		DeepSeekMessage userChatMessage = DeepSeekMessage.ofUser(userMessage);

		JsonProperty llmGrade = new JsonProperty("llm_grade", Integer.class);
		JsonProperty llmFeedback = new JsonProperty("llm_feedback", String.class);

		return deepSeekClient.responseJson(AiScoringFeedbackDto.class, DeepSeekModel.DEEPSEEK_REASONER,
				new ArrayList<>(List.of(systemChatMessage, userChatMessage)), List.of(llmGrade, llmFeedback), 0.0);
	}

	private Pair<Integer, AiScoringFeedbackDto> deepSeekFewZeroShotScoring(List<AnswerScoreDto> exampleAnswerScore,
			String scoringGuide, String question, String answer) {
		String systemMessage = PromptConstant.InstructionSystemMessage.replace("{{scoring guide}}", scoringGuide)
			.replace("{{question}}", question);

		DeepSeekMessage systemChatMessage = DeepSeekMessage.ofSystem(systemMessage);

		String userMessage = getFewShotChatMessage(exampleAnswerScore, answer).message();
		;

		DeepSeekMessage userChatMessage = DeepSeekMessage.ofUser(userMessage);

		JsonProperty llmGrade = new JsonProperty("llm_grade", Integer.class);
		JsonProperty llmFeedback = new JsonProperty("llm_feedback", String.class);

		return deepSeekClient.responseJson(AiScoringFeedbackDto.class, DeepSeekModel.DEEPSEEK_CHAT,
				new ArrayList<>(List.of(systemChatMessage, userChatMessage)), List.of(llmGrade, llmFeedback), 0.0);
	}

	private @NotNull ChatMessage getFewShotChatMessage(List<AnswerScoreDto> exampleAnswerScore, String answer) {
		StringBuilder userMessageBuilder = new StringBuilder();

		for (AnswerScoreDto answerScoreDto : exampleAnswerScore) {
			userMessageBuilder
				.append(PromptConstant.FewShotExampleUserMessage.replace("{{example answer}}", answerScoreDto.answer)
					.replace("{{example point}}", String.valueOf(answerScoreDto.getOriginalScore())));
		}

		userMessageBuilder.append(PromptConstant.ZeroShotUserMessage.replace("{{student answer}}", answer));

		String userMessage = userMessageBuilder.toString();

		return new ChatMessage(UserRole.USER, userMessage);
	}

	@Override
	public Map<String, List<AnswerScoreDto>> matchResultWithScore(List<StudentAnswerDto> studentAnswers,
			List<StudentGradeDto> studentGrades) {

		Map<String, List<AnswerScoreDto>> answerScores = new HashMap<>();

		for (StudentAnswerDto studentAnswer : studentAnswers) {

			Optional<StudentGradeDto> relatedGradeOpt = studentGrades.stream()
				.filter(g -> g.getStudentId().equals(studentAnswer.getStudentId()))
				.findFirst();
			if (relatedGradeOpt.isEmpty()) {
				System.out.println("No corresponding grade for student's " + studentAnswer.getFullName() + " answer.");
				continue;
			}

			StudentGradeDto studentGradeDto = relatedGradeOpt.get();

			AnswerScoreDto answerScoreDto1 = AnswerScoreDto.builder()
				.semester(studentAnswer.getSemester())
				.faculty(studentAnswer.getFaculty())
				.level(studentAnswer.getLevel())
				.fullName(studentAnswer.getFullName())
				.studentId(studentAnswer.getStudentId())
				.email(studentAnswer.getEmail())
				.questionType(QuestionType.QUESTION_1)
				.answer(studentAnswer.getResponseQuestion1())
				.finalScore(studentGradeDto.getQuestion1Score())
				.originalScore(QuestionType.QUESTION_1.getOriginalScore(studentGradeDto.getQuestion1Score()))
				.build();

			AnswerScoreDto answerScoreDto2 = AnswerScoreDto.builder()
				.semester(studentAnswer.getSemester())
				.faculty(studentAnswer.getFaculty())
				.level(studentAnswer.getLevel())
				.fullName(studentAnswer.getFullName())
				.studentId(studentAnswer.getStudentId())
				.email(studentAnswer.getEmail())
				.questionType(QuestionType.QUESTION_2)
				.answer(studentAnswer.getResponseQuestion2())
				.finalScore(studentGradeDto.getQuestion2Score())
				.originalScore(QuestionType.QUESTION_2.getOriginalScore(studentGradeDto.getQuestion2Score()))
				.build();

			AnswerScoreDto answerScoreDto3 = AnswerScoreDto.builder()
				.semester(studentAnswer.getSemester())
				.faculty(studentAnswer.getFaculty())
				.level(studentAnswer.getLevel())
				.fullName(studentAnswer.getFullName())
				.studentId(studentAnswer.getStudentId())
				.email(studentAnswer.getEmail())
				.questionType(QuestionType.QUESTION_3)
				.answer(studentAnswer.getResponseQuestion3())
				.finalScore(studentGradeDto.getQuestion3Score())
				.originalScore(QuestionType.QUESTION_3.getOriginalScore(studentGradeDto.getQuestion3Score()))
				.build();

			AnswerScoreDto answerScoreDto4 = AnswerScoreDto.builder()
				.semester(studentAnswer.getSemester())
				.faculty(studentAnswer.getFaculty())
				.level(studentAnswer.getLevel())
				.fullName(studentAnswer.getFullName())
				.studentId(studentAnswer.getStudentId())
				.email(studentAnswer.getEmail())
				.questionType(QuestionType.QUESTION_4)
				.answer(studentAnswer.getResponseQuestion4())
				.finalScore(studentGradeDto.getQuestion4Score())
				.originalScore(QuestionType.QUESTION_4.getOriginalScore(studentGradeDto.getQuestion4Score()))
				.build();

			List<AnswerScoreDto> answerScore = List.of(answerScoreDto1, answerScoreDto2, answerScoreDto3,
					answerScoreDto4);

			answerScores.put(studentAnswer.getStudentId(), answerScore);
		}

		return answerScores;
	}

}
