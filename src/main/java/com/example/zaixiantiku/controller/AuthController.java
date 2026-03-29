package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.pojo.dto.LoginDTO;
import com.example.zaixiantiku.pojo.dto.RegisterDTO;
import com.example.zaixiantiku.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证授权控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证授权", description = "用户注册与登录接口")
public class AuthController {

    private final UserService userService;

    /**
     * 用户登录接口
     * 
     * @param loginDTO 登录信息
     * @return Result 包含 token 和 userInfo
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "提交用户名和密码进行登录")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO loginDTO) {
        log.info("用户登录请求：{}", loginDTO.getUsername());
        Map<String, Object> data = userService.login(loginDTO);
        return Result.success(200, "登录成功", data);
    }

    /**
     * 用户注册接口
     * 
     * @param registerDTO 注册信息
     * @return Result
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "提交注册信息进行注册")
    public Result<String> register(@RequestBody RegisterDTO registerDTO) {
        log.info("用户注册请求：{}", registerDTO.getUsername());
        userService.register(registerDTO);
        return Result.success("注册成功，请等待审核");
    }
}
