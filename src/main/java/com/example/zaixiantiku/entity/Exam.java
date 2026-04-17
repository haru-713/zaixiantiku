package com.example.zaixiantiku.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 考试实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("exam")
public class Exam {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String examName;

    private Long paperId;

    private Long courseId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer duration;

    /**
     * 状态：0-未开始，1-进行中，2-已结束
     */
    private Integer status;

    /**
     * 是否公布成绩：0-否，1-是
     */
    private Integer publishScore;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
