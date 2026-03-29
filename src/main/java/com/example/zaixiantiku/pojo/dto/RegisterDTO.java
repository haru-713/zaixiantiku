package com.example.zaixiantiku.pojo.dto;

import lombok.Data;

/**
 * 注册数据传输对象
 */
@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String name;
    private String phone;
    private String email;
    private String avatar; // 头像URL
    private String roleCode; // 可选，默认STUDENT，教师传TEACHER
}
