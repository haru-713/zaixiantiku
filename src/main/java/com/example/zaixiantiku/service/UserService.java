package com.example.zaixiantiku.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.zaixiantiku.dto.RegisterDTO;
import com.example.zaixiantiku.model.User;

/**
 * 用户业务逻辑接口
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * @param registerDTO 注册信息
     */
    void register(RegisterDTO registerDTO);
}
