package com.example.zaixiantiku.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 考试提交 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSubmitDTO {
    private List<AnswerItem> answers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerItem {
        private Long questionId;
        private String answer;
        private Integer timeSpent;
    }
}
