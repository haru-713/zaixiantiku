package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.pojo.dto.ShareAuditDTO;
import com.example.zaixiantiku.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员：分享审核控制器
 */
@Tag(name = "管理员-分享审核")
@RestController
@RequestMapping("/admin/shares")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminShareController {

    private final ShareService shareService;

    @Operation(summary = "审核分享")
    @PutMapping("/{shareId}/audit")
    public Result<Void> auditShare(@PathVariable Long shareId, @RequestBody ShareAuditDTO auditDTO) {
        shareService.auditShare(shareId, auditDTO);
        return Result.success();
    }

    @Operation(summary = "置顶/取消置顶分享")
    @PutMapping("/{shareId}/top")
    public Result<Void> toggleShareTop(@PathVariable Long shareId, @RequestParam Integer isTop) {
        shareService.toggleShareTop(shareId, isTop);
        return Result.success();
    }

    @Operation(summary = "置顶/取消置顶评论")
    @PutMapping("/comments/{commentId}/top")
    public Result<Void> toggleCommentTop(@PathVariable Long commentId, @RequestParam Integer isTop) {
        shareService.toggleCommentTop(commentId, isTop);
        return Result.success();
    }
}
