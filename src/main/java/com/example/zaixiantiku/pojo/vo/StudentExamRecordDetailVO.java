package com.example.zaixiantiku.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "学生单次考试记录详情VO")
public class StudentExamRecordDetailVO {

    private Long recordId;
    private Long examId;
    private String examName;
    private Long courseId;
    private String courseName;
    private LocalDateTime submitTime;
    private Integer totalScore; // 学生得分
    private Integer maxScore; // 试卷总分
    private Double scoreRate; // 得分率
    private Integer status; // 考试记录状态

    @Schema(description = "题目详情")
    private List<QuestionDetail> questionDetails;
    @Schema(description = "本次考试知识点掌握情况雷达图")
    private List<StudentAnalysisVO.KnowledgeRadarVO> knowledgeRadar;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "单次考试题目详情")
    public static class QuestionDetail {
        private Long questionId;
        private String questionContent;
        private String questionType;
        private List<String> options;
        private String correctAnswer;
        private String analysis;
        private Integer score; // 该题分值
        private String userAnswer;
        private Integer userScore; // 学生该题得分
        private Boolean isCorrect;
    }
}
