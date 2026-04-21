package com.example.zaixiantiku.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 考试作弊行为记录 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheatRecordDTO {
    private Long examId;
    private Integer cheatCount;
}
