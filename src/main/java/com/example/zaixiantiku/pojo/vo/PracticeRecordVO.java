package com.example.zaixiantiku.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeRecordVO {
    private Long id;
    private Long userId;
    private Long courseId;
    private String courseName;
    private Integer totalScore;
    private Integer totalDuration;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
}
