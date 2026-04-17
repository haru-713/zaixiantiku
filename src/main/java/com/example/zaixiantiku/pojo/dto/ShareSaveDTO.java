package com.example.zaixiantiku.pojo.dto;

import lombok.Data;

/**
 * 分享保存/修改 DTO
 */
@Data
public class ShareSaveDTO {
    private String title;
    private String content;
    private Boolean isPublic; // 文档中提到 isPublic，对应到实体类中的 status (1-已发布, 0-待审核)
}
