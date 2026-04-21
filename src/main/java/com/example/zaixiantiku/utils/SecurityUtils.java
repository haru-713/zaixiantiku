package com.example.zaixiantiku.utils;

import com.example.zaixiantiku.exception.BusinessException;
import com.example.zaixiantiku.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * 安全工具类
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户
     */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUser().getId() : null;
    }

    /**
     * 必须登录，否则抛出异常
     */
    public static LoginUser requireLoginUser() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return loginUser;
    }

    /**
     * 必须是管理员，否则抛出异常
     */
    public static void requireAdmin() {
        LoginUser loginUser = requireLoginUser();
        if (!isAdmin(loginUser)) {
            throw new BusinessException(403, "权限不足，仅管理员可操作");
        }
    }

    /**
     * 判断是否为管理员
     */
    public static boolean isAdmin(LoginUser loginUser) {
        if (loginUser == null) return false;
        List<String> roleCodes = loginUser.getRoleCodes();
        return roleCodes != null && roleCodes.contains("ADMIN");
    }

    /**
     * 判断当前用户是否具有某角色
     */
    public static boolean hasRole(String roleCode) {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) return false;
        List<String> roleCodes = loginUser.getRoleCodes();
        return roleCodes != null && roleCodes.contains(roleCode);
    }
}
