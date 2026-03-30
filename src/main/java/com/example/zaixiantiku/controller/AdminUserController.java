package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.pojo.dto.UserAuditDTO;
import com.example.zaixiantiku.pojo.dto.UserQueryDTO;
import com.example.zaixiantiku.pojo.dto.UserStatusDTO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.UserAdminVO;
import com.example.zaixiantiku.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员用户管理控制器
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "管理员-用户管理", description = "管理员对用户的管理接口")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 分页查询用户列表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "用户列表 (分页)", description = "分页查询所有用户信息，支持模糊搜索与角色、状态过滤")
    public Result<PageResult<UserAdminVO>> getUserPage(UserQueryDTO queryDTO) {
        PageResult<UserAdminVO> pageResult = adminUserService.getUserPage(queryDTO);
        return Result.success(pageResult);
    }

    @PutMapping("/{userId}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "审核学生", description = "管理员审核学生用户：1-通过，2-拒绝")
    public Result<Void> auditStudent(@PathVariable Long userId, @RequestBody UserAuditDTO auditDTO) {
        adminUserService.auditStudent(userId, auditDTO);
        return Result.success(1, "审核完成", null);
    }

    @PutMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "启用/禁用用户", description = "管理员启用/禁用用户：0-禁用，1-启用")
    public Result<Void> updateUserStatus(@PathVariable Long userId, @RequestBody UserStatusDTO statusDTO) {
        adminUserService.updateUserStatus(userId, statusDTO);
        return Result.success(1, "状态已更新", null);
    }
}
