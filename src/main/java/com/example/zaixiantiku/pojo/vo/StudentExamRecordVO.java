package com.example.zaixiantiku.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学生考试记录 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentExamRecordVO {
    private Long id;
    private Long examId;
    private String examName;
    private Integer totalScore;
    private Integer maxScore;
    private Double scoreRate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime submitTime;
    
    /**
     * 状态：0-考试中，1-已交卷，2-已阅卷
     */
    private Integer status;
}
