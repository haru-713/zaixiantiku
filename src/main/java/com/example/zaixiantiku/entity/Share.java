package com.example.zaixiantiku.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 分享实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("share")
public class Share {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String title;
    
    private String content;
    
    /**
     * 状态：0-待审核，1-已发布，2-驳回
     */
    private Integer status;
    
    /**
     * 是否置顶：0-否，1-是
     */
    private Integer isTop;
    
    private Integer viewCount;
    
    private Integer likeCount;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
