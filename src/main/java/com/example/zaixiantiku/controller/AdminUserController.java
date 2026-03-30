package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.pojo.dto.UserQueryDTO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.UserAdminVO;
import com.example.zaixiantiku.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员用户管理控制器
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "管理员-用户管理", description = "管理员对用户的管理接口")
@PreAuthorize("hasRole('ADMIN')") // 仅管理员可访问
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 分页查询用户列表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "用户列表 (分页)", description = "分页查询所有用户信息，支持模糊搜索与角色、状态过滤")
    public Result<PageResult<UserAdminVO>> getUserPage(UserQueryDTO queryDTO) {
        PageResult<UserAdminVO> pageResult = adminUserService.getUserPage(queryDTO);
        return Result.success(pageResult);
    }
}
