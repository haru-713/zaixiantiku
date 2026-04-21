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
    // 1. 练习概况
    private Integer totalPracticeCount; // 总练习次数
    private Integer totalPracticeQuestions; // 累计答题数
    private Double avgPracticeAccuracy; // 平均正确率

    // 2. 考试概况
    private Integer totalExamCount; // 参加考试次数
    private Double avgExamScoreRate; // 平均得分率
    private Integer maxExamScore; // 最高得分
    private Integer maxExamTotalScore; // 最高分对应考试的满分

    // 3. 错题统计
    private Integer mistakeCount; // 当前错题总数

    // 4. 图表数据
    private List<TrendVO> practiceTrend; // 学习正确率趋势 (近7天)
    private List<TypeStatVO> examTypeStats; // 题型分布统计 (按正确率)
    private List<KnowledgeRadarVO> knowledgeRadar; // 知识点掌握情况 (雷达图/柱状图)

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TrendVO {
        private String date;
        private Double value; // 改为 value 以便复用，既可以存正确率也可以存得分率
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KnowledgeRadarVO {
        private String name;
        private Double value;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TypeStatVO {
        private String typeName;
        private Double accuracy;
    }
}
