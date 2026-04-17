package com.example.zaixiantiku.pojo.dto;

import lombok.Data;
import java.util.List;

@Data
public class PracticeStartDTO {
    private Long courseId;
    private PracticeConfig config;

    @Data
    public static class PracticeConfig {
        private List<Integer> typeIds;
        private List<Integer> difficulties;
        private List<Long> knowledgeIds;
        private Integer questionCount;
        private Boolean timing;
        private Integer totalTimeLimit;
    }
}
