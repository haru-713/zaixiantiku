package com.example.zaixiantiku.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.zaixiantiku.entity.User;
import com.example.zaixiantiku.pojo.dto.UserQueryDTO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.UserAdminVO;

/**
 * 管理员用户业务接口
 */
public interface AdminUserService extends IService<User> {

    /**
     * 分页查询用户列表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<UserAdminVO> getUserPage(UserQueryDTO queryDTO);
}
