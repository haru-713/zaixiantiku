package com.example.zaixiantiku.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量审核题目请求体")
public class QuestionBatchAuditDTO {
    @Schema(description = "题目 ID 列表")
    private List<Long> ids;
    @Schema(description = "审核状态：2-发布，3-禁用")
    private Integer status;
    @Schema(description = "审核原因")
    private String reason;
}
