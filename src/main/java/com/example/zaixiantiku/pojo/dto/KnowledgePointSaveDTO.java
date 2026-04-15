package com.example.zaixiantiku.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "知识点创建/修改请求体")
public class KnowledgePointSaveDTO {

    private String name;

    private Long parentId;

    private Integer sortOrder;
}

