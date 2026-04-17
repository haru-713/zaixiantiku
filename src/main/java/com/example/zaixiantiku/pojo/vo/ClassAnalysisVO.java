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
    private List<QuestionAccuracyVO> questionAccuracies;

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
    public static class QuestionAccuracyVO {
        private Long questionId;
        private String content;
        private Double accuracy;
    }
}
