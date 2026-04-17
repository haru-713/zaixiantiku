package com.example.zaixiantiku.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 分享 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareVO {
    private Long id;
    private Long userId;
    private String username; // 发布人姓名
    private String title;
    private String content;
    private Integer status;
    private Integer isTop;
    private Integer viewCount;
    private Integer likeCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
