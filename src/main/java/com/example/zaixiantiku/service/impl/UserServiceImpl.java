package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.zaixiantiku.entity.Role;
import com.example.zaixiantiku.entity.StudentClass;
import com.example.zaixiantiku.entity.User;
import com.example.zaixiantiku.entity.UserRole;
import com.example.zaixiantiku.mapper.*;
import com.example.zaixiantiku.pojo.dto.LoginDTO;
import com.example.zaixiantiku.pojo.dto.PasswordUpdateDTO;
import com.example.zaixiantiku.pojo.dto.RegisterDTO;
import com.example.zaixiantiku.pojo.dto.UserUpdateDTO;
import com.example.zaixiantiku.pojo.vo.ClassVO;
import com.example.zaixiantiku.pojo.vo.UserVO;
import com.example.zaixiantiku.security.LoginUser;
import com.example.zaixiantiku.service.UserService;
import com.example.zaixiantiku.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户业务逻辑实现类
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final StudentClassMapper studentClassMapper;
    private final ClassMapper classMapper;

    @Override
    @Transactional
    public void register(RegisterDTO registerDTO) {
        // 0. 分配角色并校验，禁止通过注册接口注册管理员
        String roleCode = StringUtils.hasText(registerDTO.getRoleCode()) ? registerDTO.getRoleCode() : "STUDENT";
        if ("ADMIN".equalsIgnoreCase(roleCode)) {
            throw new RuntimeException("禁止注册管理员账号");
        }

        // 1. 检查用户名是否存在
        User existingUser = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername()));
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 2. 检查手机号是否存在
        if (StringUtils.hasText(registerDTO.getPhone())) {
            User existingPhone = this.getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, registerDTO.getPhone()));
            if (existingPhone != null) {
                throw new RuntimeException("手机号已被注册");
            }
        }

        // 3. 检查邮箱是否存在
        if (StringUtils.hasText(registerDTO.getEmail())) {
            User existingEmail = this.getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, registerDTO.getEmail()));
            if (existingEmail != null) {
                throw new RuntimeException("邮箱已被注册");
            }
        }

        // 4. 构建用户实体
        User user = User.builder()
                .username(registerDTO.getUsername())
                .password(passwordEncoder.encode(registerDTO.getPassword()))
                .name(registerDTO.getName())
                .phone(registerDTO.getPhone())
                .email(registerDTO.getEmail())
                .avatar(registerDTO.getAvatar())
                .status(1) // 1-正常
                .auditStatus(0) // 0-待审核
                .build();

        // 5. 保存用户
        this.save(user);

        // 6. 关联角色
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, roleCode));
        if (role == null) {
            throw new RuntimeException("系统角色 [" + roleCode + "] 不存在");
        }

        UserRole userRole = UserRole.builder()
                .userId(user.getId())
                .roleId(role.getId())
                .build();
        userRoleMapper.insert(userRole);
    }

    @Override
    public Map<String, Object> login(LoginDTO loginDTO) {
        // 1. 使用 AuthenticationManager 进行认证
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDTO.getUsername(), loginDTO.getPassword());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // 2. 如果认证通过，获取用户信息
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        User user = loginUser.getUser();

        // 3. 校验账号状态
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }

        // 4. 校验审核状态 (仅学生需要审核)
        if (loginUser.getRoleCodes().contains("STUDENT")) {
            if (user.getAuditStatus() == null || user.getAuditStatus() == 0) {
                throw new RuntimeException("账号正在审核中，请耐心等待");
            }
            if (user.getAuditStatus() == 2) {
                throw new RuntimeException("账号审核未通过，请联系管理员");
            }
        }

        // 5. 生成 JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("sub", user.getUsername());
        String token = jwtUtils.createToken(claims);

        // 4. 封装结果返回
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("name", user.getName());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("roles", loginUser.getRoleCodes());

        result.put("userInfo", userInfo);
        return result;
    }

    @Override
    public UserVO getCurrentUserInfo() {
        // 1. 从 SecurityContext 中获取当前认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("用户未登录");
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        User user = loginUser.getUser();

        // 获取用户的班级列表
        List<ClassVO> classVOList = new ArrayList<>();
        if (loginUser.getRoleCodes().contains("STUDENT")) {
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
        } else if (loginUser.getRoleCodes().contains("TEACHER")) {
            // 教师：获取其管辖的班级
            classVOList = classMapper.selectList(new LambdaQueryWrapper<com.example.zaixiantiku.entity.Class>()
                    .eq(com.example.zaixiantiku.entity.Class::getTeacherId, user.getId()))
                    .stream().map(c -> ClassVO.builder()
                            .id(c.getId())
                            .className(c.getClassName())
                            .grade(c.getGrade())
                            .teacherId(c.getTeacherId())
                            .createTime(c.getCreateTime())
                            .build()).collect(Collectors.toList());
        }

        // 2. 封装 VO 返回
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .auditStatus(user.getAuditStatus())
                .createTime(user.getCreateTime())
                .roles(loginUser.getRoleCodes())
                .classes(classVOList)
                .build();
    }

    @Override
    @Transactional
    public UserVO updateCurrentUserInfo(UserUpdateDTO userUpdateDTO) {
        // 1. 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("用户未登录");
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        User user = loginUser.getUser();

        // 2. 校验手机号和邮箱唯一性 (排除当前用户)
        if (StringUtils.hasText(userUpdateDTO.getPhone())) {
            User existingPhone = this.getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, userUpdateDTO.getPhone())
                    .ne(User::getId, user.getId()));
            if (existingPhone != null) {
                throw new RuntimeException("手机号已被其他账号使用");
            }
            user.setPhone(userUpdateDTO.getPhone());
        }

        if (StringUtils.hasText(userUpdateDTO.getEmail())) {
            User existingEmail = this.getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, userUpdateDTO.getEmail())
                    .ne(User::getId, user.getId()));
            if (existingEmail != null) {
                throw new RuntimeException("邮箱已被其他账号使用");
            }
            user.setEmail(userUpdateDTO.getEmail());
        }

        // 3. 更新其他字段
        if (StringUtils.hasText(userUpdateDTO.getName())) {
            user.setName(userUpdateDTO.getName());
        }
        if (StringUtils.hasText(userUpdateDTO.getAvatar())) {
            user.setAvatar(userUpdateDTO.getAvatar());
        }

        // 4. 执行更新
        this.updateById(user);

        // 5. 返回最新的用户信息
        return getCurrentUserInfo();
    }

    @Override
    @Transactional
    public void updatePassword(PasswordUpdateDTO passwordUpdateDTO) {
        // 1. 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("用户未登录");
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        User user = this.getById(loginUser.getUser().getId());

        // 2. 校验旧密码
        if (!passwordEncoder.matches(passwordUpdateDTO.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }

        // 3. 校验新密码是否与旧密码相同
        if (passwordEncoder.matches(passwordUpdateDTO.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("新密码不能与旧密码相同");
        }

        // 4. 更新新密码
        user.setPassword(passwordEncoder.encode(passwordUpdateDTO.getNewPassword()));
        this.updateById(user);
    }
}
