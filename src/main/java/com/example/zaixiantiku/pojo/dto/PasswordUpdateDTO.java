package com.example.zaixiantiku.pojo.dto;

import lombok.Data;

/**
 * 密码修改请求对象
 */
@Data
public class PasswordUpdateDTO {
    private String oldPassword;
    private String newPassword;
}
