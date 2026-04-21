package com.example.zaixiantiku.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 考试 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamVO {

    private Long id;

    private String examName;

    private Long paperId;

    private String paperName;

    private Long courseId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    private Integer duration;

    private Integer status;

    private Integer studentStatus; // 0: 未开始, 1: 进行中, 2: 已结束, 3: 已提交/已批阅

    private Integer publishScore;

    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createTime;

    private String targetClasses;

    private String targetType; // CLASS or STUDENT
}
