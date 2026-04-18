package com.example.zaixiantiku.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperVO {

    private Long id;

    private String paperName;

    private Long courseId;

    private String courseName;

    private Integer totalScore;

    private Long createBy;

    private String creatorName;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<PaperQuestionVO> questions;
}
