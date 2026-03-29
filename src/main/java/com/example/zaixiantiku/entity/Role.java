package com.example.zaixiantiku.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 角色表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("role")
public class Role {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String roleName;

    private String roleCode;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
