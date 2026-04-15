package com.example.zaixiantiku.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识点树节点")
public class KnowledgePointTreeVO {

    private Long id;

    private String name;

    private Long parentId;

    private Integer sortOrder;

    private List<KnowledgePointTreeVO> children;
}
