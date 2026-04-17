package com.example.zaixiantiku.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 班级实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("class")
public class Class {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String className;
    private String grade;
    private Long teacherId;
    private LocalDateTime createTime;
}
