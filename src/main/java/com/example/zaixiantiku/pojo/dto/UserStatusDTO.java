package com.example.zaixiantiku.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员启用/禁用用户请求体")
public class UserStatusDTO {

    @Schema(description = "账号状态：0-禁用，1-启用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
