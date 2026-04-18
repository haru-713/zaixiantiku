package com.example.zaixiantiku.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.zaixiantiku.common.annotation.OperationLog;
import com.example.zaixiantiku.entity.Log;
import com.example.zaixiantiku.mapper.LogMapper;
import com.example.zaixiantiku.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志切面类
 */
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final LogMapper logMapper;
    private final ObjectMapper objectMapper;

    @Pointcut("@annotation(com.example.zaixiantiku.common.annotation.OperationLog)")
    public void logPointcut() {
    }

    @AfterReturning(pointcut = "logPointcut()", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, Object result) {
        handleLog(joinPoint, null, result);
    }

    private void handleLog(JoinPoint joinPoint, Exception e, Object result) {
        try {
            // 获取注解信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            OperationLog annotation = signature.getMethod().getAnnotation(OperationLog.class);
            if (annotation == null)
                return;

            // 获取当前登录用户
            Long userId = null;
            try {
                Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if (principal instanceof LoginUser loginUser) {
                    userId = loginUser.getUser().getId();
                }
            } catch (Exception ignored) {
            }

            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

            // 如果 userId 仍然为空，尝试从返回值获取 (针对登录等场景)
            if (userId == null && result instanceof com.example.zaixiantiku.common.Result<?> res) {
                Object data = res.getData();
                if (data instanceof java.util.Map<?, ?> map) {
                    // 登录接口返回的数据结构中包含 userInfo，其中有 id
                    Object userInfo = map.get("userInfo");
                    if (userInfo instanceof java.util.Map<?, ?> userMap) {
                        Object id = userMap.get("id");
                        if (id instanceof Number num) {
                            userId = num.longValue();
                        }
                    }
                }
            }

            // 如果 userId 仍然为空，尝试从参数中获取 (针对登录和注册场景)
            if (userId == null) {
                Object[] args = joinPoint.getArgs();
                for (Object arg : args) {
                    String username = null;
                    if (arg instanceof com.example.zaixiantiku.pojo.dto.LoginDTO loginDTO) {
                        username = loginDTO.getUsername();
                    } else if (arg instanceof com.example.zaixiantiku.pojo.dto.RegisterDTO registerDTO) {
                        username = registerDTO.getUsername();
                    }

                    if (username != null) {
                        try {
                            // 动态获取 UserMapper 以获取用户 ID
                            com.example.zaixiantiku.mapper.UserMapper userMapper = org.springframework.web.context.support.WebApplicationContextUtils
                                    .getRequiredWebApplicationContext(request.getServletContext())
                                    .getBean(com.example.zaixiantiku.mapper.UserMapper.class);
                            com.example.zaixiantiku.entity.User user = userMapper.selectOne(
                                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.zaixiantiku.entity.User>()
                                            .eq(com.example.zaixiantiku.entity.User::getUsername, username));
                            if (user != null) {
                                userId = user.getId();
                            }
                        } catch (Exception ignored) {
                        }
                        break;
                    }
                }
            }

            // 构建日志对象
            String params = objectMapper.writeValueAsString(joinPoint.getArgs());
            // 敏感信息脱敏 (如密码)
            if (params != null) {
                params = params.replaceAll("\"password\":\"[^\"]+\"", "\"password\":\"******\"");
            }

            Log log = Log.builder()
                    .userId(userId)
                    .module(annotation.module())
                    .operation(annotation.operation())
                    .params(params)
                    .ip(request != null ? getIpAddr(request) : "unknown")
                    .createTime(LocalDateTime.now())
                    .build();

            // 保存到数据库
            logMapper.insert(log);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 获取IP地址
     */
    private String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }
}
