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
public class GlobalAnalysisVO {
    private Integer totalExams;
    private Integer totalStudents;
    private Integer totalUsers;
    private Integer activeToday;
    private Integer totalQuestions;
    private Double averageScore;
    private Double passRate;
    private List<ClassPerformanceVO> classPerformance;
    private List<ExamBriefVO> recentExams;
    private List<TrendDataVO> scoreTrend;
    private List<TrendDataVO> passRateTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamBriefVO {
        private Long id;
        private String examName;
        private Double averageScore;
        private Integer paperTotalScore; // 试卷总分
        private Integer maxScore; // 学生最高分
        private Double passRate;
        private Double excellentRate; // 优秀率
        private Integer minScore; // 学生最低分
        private Integer participantCount;
        private String status; // 新增状态标识：'empty' - 无人参加, 'normal' - 正常
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDataVO {
        private String examName;
        private Double value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassPerformanceVO {
        private Long classId;
        private String className;
        private Double averageScore;
        private Integer maxScore;
        private Double passRate;
    }
}
