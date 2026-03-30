package com.example.zaixiantiku.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员审核学生请求体")
public class UserAuditDTO {

  @Schema(description = "审核状态：1-通过，2-拒绝", requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer auditStatus;

  @Schema(description = "审核原因/说明")
  private String reason;
}
