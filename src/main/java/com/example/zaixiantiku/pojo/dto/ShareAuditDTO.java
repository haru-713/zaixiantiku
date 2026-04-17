package com.example.zaixiantiku.pojo.dto;

import lombok.Data;

/**
 * 分享审核 DTO
 */
@Data
public class ShareAuditDTO {
    /**
     * 1-通过，2-驳回
     */
    private Integer status;
    private String reason;
}
