package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.pojo.dto.PasswordUpdateDTO;
import com.example.zaixiantiku.pojo.dto.UserUpdateDTO;
import com.example.zaixiantiku.service.UserService;
import com.example.zaixiantiku.pojo.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户个人信息相关接口")
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户信息
     * @return Result
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "根据 Token 获取当前登录用户的详细信息")
    public Result<UserVO> getCurrentUser() {
        UserVO userVO = userService.getCurrentUserInfo();
        return Result.success(userVO);
    }

    /**
     * 修改当前登录用户信息
     * @param userUpdateDTO 修改信息
     * @return Result
     */
    @PutMapping("/me")
    @Operation(summary = "修改个人信息", description = "修改当前登录用户的个人信息")
    public Result<UserVO> updateCurrentUser(@RequestBody UserUpdateDTO userUpdateDTO) {
        UserVO userVO = userService.updateCurrentUserInfo(userUpdateDTO);
        return Result.success(userVO);
    }

    /**
     * 修改当前登录用户密码
     * @param passwordUpdateDTO 密码修改信息
     * @return Result
     */
    @PutMapping("/me/password")
    @Operation(summary = "修改密码", description = "修改当前登录用户的登录密码")
    public Result<String> updatePassword(@RequestBody PasswordUpdateDTO passwordUpdateDTO) {
        userService.updatePassword(passwordUpdateDTO);
        return Result.success("密码修改成功");
    }
}
