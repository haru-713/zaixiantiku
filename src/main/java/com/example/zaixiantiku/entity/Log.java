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
 * 操作日志实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("log")
public class Log {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String operation;
    
    private String module;
    
    private String params;
    
    private String ip;
    
    private LocalDateTime createTime;
}
