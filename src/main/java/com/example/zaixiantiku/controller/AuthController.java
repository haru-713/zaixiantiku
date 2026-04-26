package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.annotation.OperationLog;
import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.pojo.dto.LoginDTO;
import com.example.zaixiantiku.pojo.dto.RegisterDTO;
import com.example.zaixiantiku.service.UserService;
import com.example.zaixiantiku.utils.RedisUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
    private final RedisUtils redisUtils;

    /**
     * 用户退出登录
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "将当前 Token 加入黑名单")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        if (org.springframework.util.StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
            // 场景2：JWT 黑名单实现
            // 将 Token 存入 Redis，设置过期时间为 24 小时（或根据 JWT 的剩余有效期设置）
            String blacklistKey = "jwt:blacklist:" + token;
            redisUtils.set(blacklistKey, "logout", 24, java.util.concurrent.TimeUnit.HOURS);
        }
        return Result.success();
    }

    /**
     * 检查 Token 是否有效
     * 
     * @return Result
     */
    @GetMapping("/check")
    @Operation(summary = "检查 Token 有效性", description = "用于前端验证 Token 是否过期")
    public Result<String> checkToken() {
        return Result.success("Token 有效");
    }

    /**
     * 用户登录接口
     * 
     * @param loginDTO 登录信息
     * @return Result 包含 token 和 userInfo
     */
    @PostMapping("/login")
    @OperationLog(module = "认证模块", operation = "用户登录")
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
    @OperationLog(module = "认证模块", operation = "用户注册")
    @Operation(summary = "用户注册", description = "提交注册信息进行注册")
    public Result<String> register(@RequestBody RegisterDTO registerDTO) {
        log.info("用户注册请求：{}", registerDTO.getUsername());
        userService.register(registerDTO);
        return Result.success("注册成功，请等待审核");
    }
}
