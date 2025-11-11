package org.ui.thesis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.genai.Client;
import org.apache.commons.lang3.tuple.Pair;
import org.ui.thesis.clients.deepseek.DeepSeekClient;
import org.ui.thesis.clients.deepseek.DeepSeekClientImpl;
import org.ui.thesis.clients.gemini.GeminiClient;
import org.ui.thesis.clients.gemini.GeminiClientImpl;
import org.ui.thesis.clients.openai.OpenAiClient;
import org.ui.thesis.clients.openai.OpenAiClientImpl;
import org.ui.thesis.dtos.AiScoringResult;
import org.ui.thesis.enums.AiModel;
import org.ui.thesis.enums.PromptTechnique;
import org.ui.thesis.enums.QuestionType;
import org.ui.thesis.services.studentscoring.StudentScoring;
import org.ui.thesis.services.studentscoring.StudentScoringImpl;
import org.ui.thesis.services.studentscoring.dto.AiScoringFeedbackDto;
import org.ui.thesis.services.studentscoring.dto.AnswerScoreDto;
import org.ui.thesis.services.studentscoring.dto.StudentAnswerDto;
import org.ui.thesis.services.studentscoring.dto.StudentGradeDto;
import org.ui.thesis.utils.JsonFileUtil;
import org.ui.thesis.utils.TextFileUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Main {

	private static final String INPUT_FILE_PATH_ANSWER = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/WRITING TEST/GROUPED RESPONSE (use this)/SEMESTER 6 GABUNGAN FK FKG ELITEP/Accumulated Responses to Use.json";

	private static final String INPUT_FILE_PATH_GRADE = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/WRITING TEST/GROUPED RESPONSE (use this)/SEMESTER 6 GABUNGAN FK FKG ELITEP/Accumulated Grade to Use.json";

	private static final String OUTPUT_FILE_PATH_ANSWER_GRADE = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/WRITING TEST/GROUPED RESPONSE (use this)/SEMESTER 6 GABUNGAN FK FKG ELITEP/Matched Responses to Grade.json";

	// ====

	private static final String INPUT_FILE_PATH_ZERO_SHOT = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Result/Zero Shot Sample/Zero Shot Sample to Process.json";

	private static final String INPUT_FILE_PATH_FEW_SHOT = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Result/Few Shot Sample/Few Shot Sample to Process.json";

	private static final String INPUT_EXAMPLE_FILE_PATH_FEW_SHOT = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Result/Few Shot Sample/Few Shot Example Prompt.json";

	private static final String INPUT_QUESTION_3_FILE_PATH = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Question 3 Advanced.txt";

	private static final String INPUT_QUESTION_3_SCORING_GUIDE_FILE_PATH = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Question 3 Scoring Guide.txt";

	private static final String OUTPUT_QUESTION_3_AI_FEEDBACK_FILE_PATH = "E:/Cool Yeah/KA Ultimate/Bahan/Data Gathering/Result/Few Shot Sample/Sample Result.json";

	private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());

	public static void main(String[] args) {
		GeminiClient geminiClient = new GeminiClientImpl(Client.builder().build());
		DeepSeekClient deepSeekClient = new DeepSeekClientImpl();
		OpenAiClient openAiClient = new OpenAiClientImpl();

		StudentScoring studentScoring = new StudentScoringImpl(deepSeekClient, openAiClient, geminiClient);

		Map<String, List<AnswerScoreDto>> processedRecords = JsonFileUtil.readJsonByReference(INPUT_FILE_PATH_FEW_SHOT,
				new TypeReference<>() {
				});

		List<AnswerScoreDto> exampleFewShot = JsonFileUtil.readJsonArrayFromFile(INPUT_EXAMPLE_FILE_PATH_FEW_SHOT,
				AnswerScoreDto.class);

		String scoringGuide = TextFileUtil.readAllText(INPUT_QUESTION_3_SCORING_GUIDE_FILE_PATH);

		String question = TextFileUtil.readAllText(INPUT_QUESTION_3_FILE_PATH);

		List<AiScoringResult> feedbackRecords = new ArrayList<>();

		AiModel aiModel = AiModel.CHATGPT;
		PromptTechnique promptTechnique = PromptTechnique.FEW_SHOT;

		for (List<AnswerScoreDto> answerScoreList : Objects.requireNonNull(processedRecords).values()) {
			AnswerScoreDto answerScoreQuestion3 = answerScoreList.stream()
				.filter(a -> a.getQuestionType().equals(QuestionType.QUESTION_3))
				.toList()
				.getFirst();
			if (answerScoreQuestion3 != null) {
				Pair<Integer, AiScoringFeedbackDto> feedbackDtoMap = studentScoring.getAiScoreAndFeedback(aiModel,
						promptTechnique, exampleFewShot, scoringGuide, question, answerScoreQuestion3.getAnswer());
				AiScoringFeedbackDto feedbackDto = feedbackDtoMap.getRight();
				AiScoringResult aiScoringResult = AiScoringResult.builder()
					.semester(answerScoreQuestion3.getSemester())
					.faculty(answerScoreQuestion3.getFaculty())
					.level(answerScoreQuestion3.getLevel())
					.fullName(answerScoreQuestion3.getFullName())
					.studentId(answerScoreQuestion3.getStudentId())
					.questionType(QuestionType.QUESTION_3)
					.answer(answerScoreQuestion3.getAnswer())
					.aiModel(aiModel)
					.promptTechnique(promptTechnique)
					.aiScore(feedbackDto.getLlm_grade())
					.aiFeedback(feedbackDto.getLlm_feedback())
					.tokenUsage(feedbackDtoMap.getLeft())
					.build();
				feedbackRecords.add(aiScoringResult);
			}
		}

		JsonFileUtil.writeObjectToFile(feedbackRecords, OUTPUT_QUESTION_3_AI_FEEDBACK_FILE_PATH);

		System.out.println("Exported " + feedbackRecords.size() + " records.");
	}

	private void importAndMatchData() {
		System.out.println("--- 1. IMPORT DATA ---");

		List<StudentAnswerDto> importedStudentAnswerDto = JsonFileUtil.readJsonArrayFromFile(INPUT_FILE_PATH_ANSWER,
				StudentAnswerDto.class);

		if (importedStudentAnswerDto.isEmpty()) {
			System.out.println("No Student Answer records imported. Exiting.");
			return;
		}

		System.out.println("Imported " + importedStudentAnswerDto.size() + " answer records.");

		List<StudentGradeDto> importedStudentGradeDto = JsonFileUtil.readJsonArrayFromFile(INPUT_FILE_PATH_GRADE,
				StudentGradeDto.class);

		if (importedStudentGradeDto.isEmpty()) {
			System.out.println("No Student Grade records imported. Exiting.");
			return;
		}

		System.out.println("Imported " + importedStudentGradeDto.size() + " grade records.");

		System.out.println("\n--- 2. PROCESS DATA ---");

		StudentScoring studentScoringSvc = new StudentScoringImpl();

		Map<String, List<AnswerScoreDto>> processedRecords = studentScoringSvc
			.matchResultWithScore(importedStudentAnswerDto, importedStudentGradeDto);

		System.out.println("\n--- 3. EXPORT DATA ---");

		JsonFileUtil.writeObjectToFile(processedRecords, OUTPUT_FILE_PATH_ANSWER_GRADE);

		System.out.println("Exported " + processedRecords.size() + " records.");
	}

	private void aiClientHealthCheck() {
		GeminiClient geminiClient = new GeminiClientImpl(Client.builder().build());
		System.out.println("Gemini Result:");
		geminiClient.healthCheck();

		DeepSeekClient deepSeekClient = new DeepSeekClientImpl();
		System.out.println("DeepSeek Result:");
		deepSeekClient.healthCheck();

		OpenAiClient openAiClient = new OpenAiClientImpl();
		System.out.println("OpenAi Result:");
		openAiClient.healthCheck();
	}

}