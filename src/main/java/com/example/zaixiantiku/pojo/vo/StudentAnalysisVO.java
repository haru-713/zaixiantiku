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
    // 1. 整体概览 (仪表盘)
    private Integer totalPracticeCount;      // 总练习次数
    private Integer totalPracticeQuestions; // 总练习题目数
    private Double avgPracticeAccuracy;     // 练习平均正确率
    
    private Integer totalExamCount;         // 参加考试次数
    private Double avgExamScore;            // 考试平均分
    private Integer maxExamScore;           // 考试最高分
    private Double avgExamScoreRate;        // 平均得分率
    private Integer mistakeCount;           // 错题总数

    // 2. 详细记录列表
    private List<RecentExamVO> recentExams; // 近期考试记录

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RecentExamVO {
        private String examName;
        private Integer score;
        private Integer maxScore;
        private Double scoreRate;
        private String submitTime;
    }

    // 3. 练习详细分析
    private List<TrendVO> practiceTrend;             // 近期练习正确率趋势 (折线图)
    private List<KnowledgeMasteryVO> knowledgeRadar; // 知识点掌握情况 (雷达图)
    private List<TypeStatVO> practiceTypeStats;      // 各题型练习统计
    private List<MistakePointVO> topMistakePoints;   // 错题集中知识点 Top5

    // 3. 考试详细分析
    private List<ScoreDistVO> examScoreDist;        // 考试成绩分布 (柱状图)
    private List<RankingTrendVO> rankingTrend;      // 班级排名趋势 (折线图)
    private List<TypeStatVO> examTypeStats;         // 各题型得分率统计
    private List<HighFreqMistakeVO> highFreqMistakes; // 考试高频错题

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TrendVO {
        private String date;
        private Double accuracy;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KnowledgeMasteryVO {
        private String name;
        private Double value; // 正确率
        private Integer total; // 该知识点题目总数
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TypeStatVO {
        private String typeName;
        private Integer count;
        private Double accuracy; // 或得分率
        private Double classAvg; // 班级平均 (用于考试)
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MistakePointVO {
        private String name;
        private Integer count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScoreDistVO {
        private String range;
        private Integer count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RankingTrendVO {
        private String examName;
        private Integer rank;
        private Integer totalStudents;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HighFreqMistakeVO {
        private Long questionId;
        private String content;
        private Integer wrongCount;
    }
}
