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
public class StudentAnalysisVO {
    private Integer totalPracticeCount;
    private Integer totalQuestionCount;
    private Integer mistakeCount;
    private Double avgAccuracy;
    private List<TrendVO> trend;
    private List<KnowledgeMasteryVO> knowledgeMastery;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendVO {
        private String date;
        private Double accuracy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeMasteryVO {
        private String knowledgeName;
        private Double accuracy;
    }
}
