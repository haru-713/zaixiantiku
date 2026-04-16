package com.example.zaixiantiku.pojo.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AutoGenerateRuleDTO {
    private List<TypeDistributionDTO> typeDistribution;
    private Map<String, Double> difficultyRatio; // "1": 0.3, "2": 0.5, "3": 0.2
    private List<Long> knowledgeIds;

    @Data
    public static class TypeDistributionDTO {
        private Integer typeId;
        private Integer count;
        private Integer scorePerQuestion;
    }
}
