package com.example.zaixiantiku.pojo.dto;

import lombok.Data;

import java.util.List;

/**
 * 考试阅卷 DTO
 */
@Data
public class ExamMarkDTO {
    private List<MarkItem> scores;

    @Data
    public static class MarkItem {
        private Long questionId;
        private Integer score;
    }
}
