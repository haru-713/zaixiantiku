package com.example.zaixiantiku.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassAnalysisVO {
    private List<ScoreDistributionVO> scoreDistribution;
    private Double averageScore;
    private Integer maxScore;
    private List<QuestionAccuracyVO> questionAccuracies;
    private List<StudentScoreVO> studentScores;
    private List<TrendVO> classTrend; // 班级历史成绩趋势

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendVO {
        private String examName;
        private Double averageScore;
        private Double passRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreDistributionVO {
        private String range;
        private Integer count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentScoreVO {
        private String username;
        private String name;
        private String className;
        private Integer score;
        private Integer totalScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionAccuracyVO {
        private Long questionId;
        private String content;
        private Double accuracy;
    }
}
