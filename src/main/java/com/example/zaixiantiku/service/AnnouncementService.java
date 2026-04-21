package com.example.zaixiantiku.service;

import com.example.zaixiantiku.entity.Announcement;
import com.example.zaixiantiku.pojo.vo.PageResult;

/**
 * 公告管理服务接口
 */
public interface AnnouncementService {
    
    /**
     * 发布公告
     */
    Announcement saveAnnouncement(Announcement announcement);
    
    /**
     * 修改公告
     */
    Announcement updateAnnouncement(Long id, Announcement announcement);
    
    /**
     * 删除公告
     */
    void deleteAnnouncement(Long id);
    
    /**
     * 获取公告列表（支持分页和权限过滤）
     */
    PageResult<Announcement> getAnnouncements(Integer isTop, Integer page, Integer size, boolean filterExpired);

    /**
     * 批量删除过期公告
     */
    void deleteExpiredAnnouncements();
}
