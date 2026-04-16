package com.example.zaixiantiku.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "题目审核请求体")
public class QuestionAuditDTO {
    @Schema(description = "状态：2-发布，3-禁用")
    private Integer status;
    @Schema(description = "审核原因")
    private String reason;
}
