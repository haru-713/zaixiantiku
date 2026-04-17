package com.example.zaixiantiku.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 考试记录详情 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamRecordDetailVO {
    private String examName;
    private Integer totalScore;
    private List<AnswerItemVO> answers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerItemVO {
        private Long questionId;
        private String content;
        private String userAnswer;
        private String correctAnswer;
        private Integer score;
        private Boolean isCorrect;
        private String analysis;
        private List<String> options;
        private String typeName;
    }
}
