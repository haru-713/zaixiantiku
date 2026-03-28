package com.example.zaixiantiku.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.zaixiantiku.dto.LoginDTO;
import com.example.zaixiantiku.dto.RegisterDTO;
import com.example.zaixiantiku.model.User;

import java.util.Map;

/**
 * 用户业务逻辑接口
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * @param registerDTO 注册信息
     */
    void register(RegisterDTO registerDTO);

    /**
     * 用户登录
     * @param loginDTO 登录信息
     * @return 包含 token 和 userInfo 的结果集
     */
    Map<String, Object> login(LoginDTO loginDTO);
}
