package org.ui.thesis.constants;

public class PromptConstant {

	public static final String InstructionSystemMessage = "You are a teacher for English Course, currently grading a writing assessment. The question you give to your students is: {{question}}. You have scoring guide as follows: {{scoring guide}}. Score the students’ answer and give your reason as feedback. Return your final evaluation strictly as valid JSON with exactly two keys: 'llm_grade' (an integer for the score) and 'llm_feedback' (a string for your feedback). Do not include any extra text or your reasoning process.";

	public static final String UserMessage = "Score this student's answer: {{student answer}}";

	public static final String FewShotExampleUserMessage = "Here is a student's answer: {{example answer}}. That answer got {{example point}} point. ";

	public static final String ChainOfThoughtExampleUserMessage = "Here is a student's answer: {{example answer}}. That answer got {{example point}} point because of the following reasons: {{example reason}}. ";

}
