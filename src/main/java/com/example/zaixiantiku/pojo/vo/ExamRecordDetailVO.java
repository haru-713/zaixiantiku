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
    private Integer maxScore;
    private Integer rank;           // 班级排名
    private Integer totalStudents;  // 总人数
    private List<KnowledgeMasteryVO> knowledgeMastery; // 知识点掌握情况
    private List<AnswerItemVO> answers;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KnowledgeMasteryVO {
        private String name;
        private Double accuracy;
    }

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
