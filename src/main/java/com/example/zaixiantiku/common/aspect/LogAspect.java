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
        handleLog(joinPoint, null);
    }

    private void handleLog(JoinPoint joinPoint, Exception e) {
        try {
            // 获取注解信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            OperationLog annotation = signature.getMethod().getAnnotation(OperationLog.class);
            if (annotation == null)
                return;

            // 获取当前登录用户
            Long userId = null;
            try {
                LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                userId = loginUser.getUser().getId();
            } catch (Exception ignored) {
            }

            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

            // 构建日志对象
            Log log = Log.builder()
                    .userId(userId)
                    .module(annotation.module())
                    .operation(annotation.operation())
                    .params(objectMapper.writeValueAsString(joinPoint.getArgs()))
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
