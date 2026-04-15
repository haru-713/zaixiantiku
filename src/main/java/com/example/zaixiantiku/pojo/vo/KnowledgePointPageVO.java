package com.example.zaixiantiku.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识点分页列表项")
public class KnowledgePointPageVO {

    private Long id;

    private Long courseId;

    private String courseName;

    private String name;

    private Long parentId;

    private Integer sortOrder;

    private LocalDateTime createTime;
}

