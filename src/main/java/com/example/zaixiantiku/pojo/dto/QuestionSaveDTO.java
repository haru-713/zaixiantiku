package com.example.zaixiantiku.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "题目创建/修改请求体")
public class QuestionSaveDTO {

    private Long courseId;

    private Integer typeId;

    private String content;

    private Integer difficulty;

    private List<String> options;

    private String answer;

    private String analysis;

    private List<Long> knowledgeIds;
}

