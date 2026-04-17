package com.example.zaixiantiku.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shareId;

    private Long userId;

    private String content;
    
    /**
     * 是否置顶：0-否，1-是
     */
    private Integer isTop;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
