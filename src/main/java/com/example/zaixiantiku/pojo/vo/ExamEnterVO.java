package com.example.zaixiantiku.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 进入考试 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamEnterVO {
    private Long examId;
    private String examName;
    private PaperInfo paper;
    private Long remainingSeconds;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaperInfo {
        private Long id;
        private String paperName;
        private List<QuestionInfo> questions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionInfo {
        private Long id;
        private String content;
        private Integer typeId;
        private List<String> options;
        private Integer sortOrder;
        private Integer score;
    }
}
