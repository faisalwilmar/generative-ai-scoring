package org.ui.thesis.services.studentscoring;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.ui.thesis.enums.QuestionType;
import org.ui.thesis.services.studentscoring.dto.AnswerScoreDto;
import org.ui.thesis.services.studentscoring.dto.StudentAnswerDto;
import org.ui.thesis.services.studentscoring.dto.StudentGradeDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor
public class StudentScoringImpl implements StudentScoring {

    @Override
    public Map<String, List<AnswerScoreDto>> matchResultWithScore(List<StudentAnswerDto> studentAnswers, List<StudentGradeDto> studentGrades){

        Map<String, List<AnswerScoreDto>> answerScores = new HashMap<>();

        for (StudentAnswerDto studentAnswer : studentAnswers){

            Optional<StudentGradeDto> relatedGradeOpt = studentGrades.stream().filter(g -> g.getStudentId().equals(studentAnswer.getStudentId())).findFirst();
            if (relatedGradeOpt.isEmpty()){
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

            List<AnswerScoreDto> answerScore = List.of(answerScoreDto1, answerScoreDto2, answerScoreDto3, answerScoreDto4);

            answerScores.put(studentAnswer.getStudentId(), answerScore);
        }

        return answerScores;
    }
}
