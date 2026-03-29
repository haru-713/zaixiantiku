package com.example.zaixiantiku.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.zaixiantiku.pojo.dto.LoginDTO;
import com.example.zaixiantiku.pojo.dto.PasswordUpdateDTO;
import com.example.zaixiantiku.pojo.dto.RegisterDTO;
import com.example.zaixiantiku.pojo.dto.UserUpdateDTO;
import com.example.zaixiantiku.pojo.vo.UserVO;
import com.example.zaixiantiku.entity.User;

import java.util.Map;

/**
 * 用户业务逻辑接口
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * 
     * @param registerDTO 注册信息
     */
    void register(RegisterDTO registerDTO);

    /**
     * 用户登录
     * 
     * @param loginDTO 登录信息
     * @return 包含 token 和 userInfo 的结果集
     */
    Map<String, Object> login(LoginDTO loginDTO);

    /**
     * 获取当前登录用户信息
     * 
     * @return 用户详细信息
     */
    UserVO getCurrentUserInfo();

    /**
     * 修改当前登录用户信息
     * 
     * @param userUpdateDTO 修改信息
     * @return 修改后的用户详细信息
     */
    UserVO updateCurrentUserInfo(UserUpdateDTO userUpdateDTO);

    /**
     * 修改当前登录用户密码
     * 
     * @param passwordUpdateDTO 密码修改信息
     */
    void updatePassword(PasswordUpdateDTO passwordUpdateDTO);
}
