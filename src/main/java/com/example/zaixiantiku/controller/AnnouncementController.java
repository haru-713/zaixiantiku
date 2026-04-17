package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.annotation.OperationLog;
import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.entity.Announcement;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 公告管理控制器
 */
@Tag(name = "公告管理")
@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "发布公告")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(module = "公告管理", operation = "发布公告")
    public Result<Announcement> saveAnnouncement(@RequestBody Announcement announcement) {
        return Result.success(announcementService.saveAnnouncement(announcement));
    }

    @Operation(summary = "修改公告")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(module = "公告管理", operation = "修改公告")
    public Result<Announcement> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement announcement) {
        return Result.success(announcementService.updateAnnouncement(id, announcement));
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(module = "公告管理", operation = "删除公告")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return Result.success();
    }

    @Operation(summary = "获取公告列表")
    @GetMapping
    public Result<PageResult<Announcement>> getAnnouncements(
            @RequestParam(required = false) Integer isTop,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(announcementService.getAnnouncements(isTop, page, size));
    }
}
