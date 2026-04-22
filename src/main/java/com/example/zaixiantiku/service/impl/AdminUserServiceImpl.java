package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.zaixiantiku.entity.StudentClass;
import com.example.zaixiantiku.entity.User;
import com.example.zaixiantiku.mapper.*;
import com.example.zaixiantiku.pojo.dto.UserAuditDTO;
import com.example.zaixiantiku.pojo.dto.UserQueryDTO;
import com.example.zaixiantiku.pojo.dto.UserStatusDTO;
import com.example.zaixiantiku.pojo.vo.ClassVO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.UserAdminVO;
import com.example.zaixiantiku.security.LoginUser;
import com.example.zaixiantiku.service.AdminUserService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理员用户业务实现类
 */
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl extends ServiceImpl<UserMapper, User> implements AdminUserService {

    private final UserMapper userMapper;
    private final StudentClassMapper studentClassMapper;
    private final ClassMapper classMapper;
    private final PasswordEncoder passwordEncoder;
    private static final Set<String> ALLOWED_ROLE_CODES = Set.of("STUDENT", "TEACHER", "ADMIN");

    @Override
    public PageResult<UserAdminVO> getUserPage(UserQueryDTO queryDTO) {
        // 1. 开启分页
        Integer page = queryDTO.getPage() == null || queryDTO.getPage() < 1 ? 1 : queryDTO.getPage();
        Integer size = queryDTO.getSize() == null || queryDTO.getSize() < 1 ? 10 : queryDTO.getSize();
        PageHelper.startPage(page, size);

        // 2. 构建查询条件
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        boolean forceStudentOnly = false;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            List<String> callerRoles = loginUser.getRoleCodes();
            if (callerRoles != null && callerRoles.contains("TEACHER") && !callerRoles.contains("ADMIN")) {
                forceStudentOnly = true;
            }
        }

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
        if (forceStudentOnly) {
            roleCode = "STUDENT";
        }
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

            // 获取学生的班级列表
            List<ClassVO> classVOList = new ArrayList<>();
            if (roleCodes.contains("STUDENT")) {
                List<Long> classIds = studentClassMapper.selectList(new LambdaQueryWrapper<StudentClass>()
                        .eq(StudentClass::getStudentId, user.getId()))
                        .stream().map(StudentClass::getClassId).collect(Collectors.toList());
                if (!classIds.isEmpty()) {
                    classVOList = classMapper.selectBatchIds(classIds).stream().map(c -> ClassVO.builder()
                            .id(c.getId())
                            .className(c.getClassName())
                            .grade(c.getGrade())
                            .teacherId(c.getTeacherId())
                            .createTime(c.getCreateTime())
                            .build()).collect(Collectors.toList());
                }
            }

            return UserAdminVO.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .name(user.getName())
                    .phone(user.getPhone())
                    .email(user.getEmail())
                    .avatar(user.getAvatar())
                    .status(user.getStatus())
                    .auditStatus(user.getAuditStatus())
                    .createTime(user.getCreateTime())
                    .roleCodes(roleCodes)
                    .classes(classVOList)
                    .build();
        }).collect(Collectors.toList());

        // 5. 封装结果返回
        return PageResult.of(pageInfo.getTotal(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditStudent(Long userId, UserAuditDTO auditDTO) {
        if (userId == null) {
            throw new RuntimeException("userId 不能为空");
        }
        if (auditDTO == null || auditDTO.getAuditStatus() == null) {
            throw new RuntimeException("auditStatus 不能为空");
        }
        if (!Objects.equals(auditDTO.getAuditStatus(), 1) && !Objects.equals(auditDTO.getAuditStatus(), 2)) {
            throw new RuntimeException("auditStatus 参数非法");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        List<String> roleCodes = userMapper.findRoleCodesByUserId(userId);
        if (roleCodes == null || !roleCodes.contains("STUDENT")) {
            throw new RuntimeException("仅支持审核学生用户");
        }

        User update = new User();
        update.setId(userId);
        update.setAuditStatus(auditDTO.getAuditStatus());
        int rows = userMapper.updateById(update);
        if (rows != 1) {
            throw new RuntimeException("审核失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, UserStatusDTO statusDTO) {
        if (userId == null) {
            throw new RuntimeException("userId 不能为空");
        }
        if (statusDTO == null || statusDTO.getStatus() == null) {
            throw new RuntimeException("status 不能为空");
        }
        if (!Objects.equals(statusDTO.getStatus(), 0) && !Objects.equals(statusDTO.getStatus(), 1)) {
            throw new RuntimeException("status 参数非法");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 限制：管理员账号不能被禁用
        List<String> targetRoles = userMapper.findRoleCodesByUserId(userId);
        if (targetRoles.contains("ADMIN")) {
            throw new RuntimeException("管理员账号不能被禁用");
        }

        if (Objects.equals(statusDTO.getStatus(), 0)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
                if (loginUser.getUser() != null && Objects.equals(loginUser.getUser().getId(), userId)) {
                    throw new RuntimeException("不能禁用当前登录账号");
                }
            }
        }

        User update = new User();
        update.setId(userId);
        update.setStatus(statusDTO.getStatus());
        int rows = userMapper.updateById(update);
        if (rows != 1) {
            throw new RuntimeException("状态更新失败");
        }
    }

    @Override
    public void resetPassword(Long userId) {
        // 1. 获取当前登录用户，防止自重置
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            if (Objects.equals(loginUser.getUser().getId(), userId)) {
                throw new RuntimeException("管理员不能重置自己的密码，请联系其他管理员协助");
            }
        }

        // 2. 查询目标用户
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 限制：管理员之间不能互相重置密码
        List<String> targetRoles = userMapper.findRoleCodesByUserId(userId);
        if (targetRoles.contains("ADMIN")) {
            throw new RuntimeException("不能重置其他管理员的密码");
        }

        // 3. 重置为初始密码 123456
        user.setPassword(passwordEncoder.encode("123456"));
        this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        if (userId == null) {
            throw new RuntimeException("userId 不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 1. 限制：不能删除管理员
        List<String> targetRoles = userMapper.findRoleCodesByUserId(userId);
        if (targetRoles.contains("ADMIN")) {
            throw new RuntimeException("管理员账号禁止通过界面删除，请通过数据库操作");
        }

        // 2. 限制：不能删除自己
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            if (Objects.equals(loginUser.getUser().getId(), userId)) {
                throw new RuntimeException("不能删除当前登录账号");
            }
        }

        // 3. 业务校验：如果是教师，检查是否有课程关联
        if (targetRoles.contains("TEACHER")) {
            // 这里可以根据实际表结构增加更多校验，如 paper, question 等
            // 为简化演示，仅校验 course_teacher
            Long courseCount = userMapper.selectCountBySql("SELECT COUNT(1) FROM course_teacher WHERE teacher_id = " + userId);
            if (courseCount != null && courseCount > 0) {
                throw new RuntimeException("该教师名下有关联课程，请先转移或删除课程后再删除用户");
            }
        }

        // 4. 执行删除 (user_role 等关联表已设置级联删除)
        userMapper.deleteById(userId);
    }
}
