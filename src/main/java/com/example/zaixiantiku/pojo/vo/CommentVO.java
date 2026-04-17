package com.example.zaixiantiku.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {
    private Long id;
    private Long shareId;
    private Long userId;
    private String username; // 评论人姓名
    private String content;
    private Integer isTop;
    private LocalDateTime createTime;
}
