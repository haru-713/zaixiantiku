package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.zaixiantiku.dto.LoginDTO;
import com.example.zaixiantiku.dto.RegisterDTO;
import com.example.zaixiantiku.mapper.RoleMapper;
import com.example.zaixiantiku.mapper.UserMapper;
import com.example.zaixiantiku.mapper.UserRoleMapper;
import com.example.zaixiantiku.model.Role;
import com.example.zaixiantiku.model.User;
import com.example.zaixiantiku.model.UserRole;
import com.example.zaixiantiku.security.LoginUser;
import com.example.zaixiantiku.service.UserService;
import com.example.zaixiantiku.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword());
        
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        
        // 2. 如果认证通过，获取用户信息
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        User user = loginUser.getUser();

        // 3. 生成 JWT Token
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
}
