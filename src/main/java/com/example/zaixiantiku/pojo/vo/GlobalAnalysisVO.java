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
    private Double averageScore;
    private Double passRate;
    private List<ClassPerformanceVO> classPerformance;
    private List<ExamBriefVO> recentExams;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamBriefVO {
        private Long id;
        private String examName;
        private Double averageScore;
        private Integer maxScore;
        private Double passRate;
        private Integer participantCount;
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
