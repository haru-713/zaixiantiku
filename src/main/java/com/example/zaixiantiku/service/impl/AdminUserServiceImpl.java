package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.zaixiantiku.entity.User;
import com.example.zaixiantiku.mapper.UserMapper;
import com.example.zaixiantiku.pojo.dto.UserQueryDTO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.UserAdminVO;
import com.example.zaixiantiku.service.AdminUserService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员用户业务实现类
 */
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl extends ServiceImpl<UserMapper, User> implements AdminUserService {

    private final UserMapper userMapper;
    private static final Set<String> ALLOWED_ROLE_CODES = Set.of("STUDENT", "TEACHER", "ADMIN");

    @Override
    public PageResult<UserAdminVO> getUserPage(UserQueryDTO queryDTO) {
        // 1. 开启分页
        Integer page = queryDTO.getPage() == null || queryDTO.getPage() < 1 ? 1 : queryDTO.getPage();
        Integer size = queryDTO.getSize() == null || queryDTO.getSize() < 1 ? 10 : queryDTO.getSize();
        PageHelper.startPage(page, size);

        // 2. 构建查询条件
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        // 模糊搜索：用户名/姓名/手机号
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            queryWrapper.and(w -> w.like(User::getUsername, queryDTO.getKeyword())
                    .or().like(User::getName, queryDTO.getKeyword())
                    .or().like(User::getPhone, queryDTO.getKeyword()));
        }

        // 状态过滤
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(User::getStatus, queryDTO.getStatus());
        }

        String roleCode = StringUtils.hasText(queryDTO.getRoleCode()) ? queryDTO.getRoleCode().trim() : null;
        if (StringUtils.hasText(roleCode) && !ALLOWED_ROLE_CODES.contains(roleCode)) {
            throw new RuntimeException("roleCode 参数非法");
        }

        Integer auditStatus = queryDTO.getAuditStatus();
        if (auditStatus != null) {
            if (!StringUtils.hasText(roleCode)) {
                queryWrapper.eq(User::getAuditStatus, auditStatus);
                queryWrapper.apply(
                        "id IN (SELECT ur.user_id FROM user_role ur JOIN role r ON ur.role_id = r.id WHERE r.role_code = 'STUDENT')");
            } else if ("STUDENT".equals(roleCode)) {
                queryWrapper.eq(User::getAuditStatus, auditStatus);
            }
        }

        if (StringUtils.hasText(roleCode)) {
            queryWrapper.apply(
                    "id IN (SELECT ur.user_id FROM user_role ur JOIN role r ON ur.role_id = r.id WHERE r.role_code = {0})",
                    roleCode);
        }

        queryWrapper.orderByAsc(User::getId);

        // 3. 执行查询
        List<User> userList = userMapper.selectList(queryWrapper);
        PageInfo<User> pageInfo = new PageInfo<>(userList);

        // 4. 转换 VO 并注入角色信息
        List<UserAdminVO> voList = userList.stream().map(user -> {
            List<String> roleCodes = userMapper.findRoleCodesByUserId(user.getId());
            return UserAdminVO.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .name(user.getName())
                    .phone(user.getPhone())
                    .email(user.getEmail())
                    .avatar(user.getAvatar())
                    .status(user.getStatus())
                    .auditStatus(user.getAuditStatus())
                    .roleCodes(roleCodes)
                    .build();
        }).collect(Collectors.toList());

        // 5. 封装结果返回
        return PageResult.of(pageInfo.getTotal(), voList);
    }
}
