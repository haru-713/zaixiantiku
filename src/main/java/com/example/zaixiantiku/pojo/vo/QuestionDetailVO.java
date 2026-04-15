package com.example.zaixiantiku.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "题目详情")
public class QuestionDetailVO {

    private Long id;

    private Long courseId;

    private Integer typeId;

    private String content;

    private Integer difficulty;

    private List<String> options;

    private String answer;

    private String analysis;

    private Integer status;

    private Long createBy;

    private List<Long> knowledgeIds;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

