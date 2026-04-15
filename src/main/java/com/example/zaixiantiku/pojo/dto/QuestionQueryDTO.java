package com.example.zaixiantiku.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "题目列表查询参数")
public class QuestionQueryDTO {

    @Schema(description = "当前页码", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", defaultValue = "10")
    private Integer size = 10;

    private Long courseId;

    private Integer typeId;

    private Integer difficulty;

    private Long knowledgeId;

    private String keyword;

    private Integer status;
}

