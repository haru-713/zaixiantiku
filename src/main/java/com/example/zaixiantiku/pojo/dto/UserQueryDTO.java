package com.example.zaixiantiku.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户查询 DTO
 */
@Data
@Schema(description = "用户列表查询参数")
public class UserQueryDTO {

    @Schema(description = "当前页码", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", defaultValue = "10")
    private Integer size = 10;

    @Schema(description = "用户名/姓名/手机号模糊搜索")
    private String keyword;

    @Schema(description = "角色过滤 (STUDENT/TEACHER/ADMIN)")
    private String roleCode;

    @Schema(description = "状态 (0-禁用, 1-正常)")
    private Integer status;

    @Schema(description = "审核状态 (针对学生: 0-待审核, 1-审核通过, 2-审核拒绝)")
    private Integer auditStatus;
}
