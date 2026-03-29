package com.example.zaixiantiku.pojo.dto;

import lombok.Data;

/**
 * 用户信息修改请求对象
 */
@Data
public class UserUpdateDTO {
    private String name;
    private String phone;
    private String email;
    private String avatar;
}
