package com.example.zaixiantiku.pojo.dto;

import lombok.Data;
import java.util.List;

@Data
public class PracticeSubmitDTO {
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Long questionId;
        private String answer;
        private Integer timeSpent;
    }
}
