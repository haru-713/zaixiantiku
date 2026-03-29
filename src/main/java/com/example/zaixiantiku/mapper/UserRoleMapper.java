package com.example.zaixiantiku.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.zaixiantiku.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联表 Mapper 接口
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
}
