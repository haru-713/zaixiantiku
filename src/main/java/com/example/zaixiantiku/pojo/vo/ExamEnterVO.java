package com.example.zaixiantiku.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

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
    private Map<Long, String> answers; // 已保存的答案：questionId -> answer
    private Integer cheatCount; // 已记录的切屏次数

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
