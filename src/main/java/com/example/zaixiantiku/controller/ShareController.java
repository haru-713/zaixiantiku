package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.entity.Share;
import com.example.zaixiantiku.pojo.dto.CommentSaveDTO;
import com.example.zaixiantiku.pojo.dto.ShareSaveDTO;
import com.example.zaixiantiku.pojo.vo.CommentVO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.ShareVO;
import com.example.zaixiantiku.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分享交流控制器
 */
@Tag(name = "分享交流")
@RestController
@RequestMapping("/shares")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @Operation(summary = "发布分享")
    @PostMapping
    public Result<Share> saveShare(@RequestBody ShareSaveDTO shareDTO) {
        return Result.success(shareService.saveShare(shareDTO));
    }

    @Operation(summary = "修改分享")
    @PutMapping("/{shareId}")
    public Result<Share> updateShare(@PathVariable Long shareId, @RequestBody ShareSaveDTO shareDTO) {
        return Result.success(shareService.updateShare(shareId, shareDTO));
    }

    @Operation(summary = "删除分享")
    @DeleteMapping("/{shareId}")
    public Result<Void> deleteShare(@PathVariable Long shareId) {
        shareService.deleteShare(shareId);
        return Result.success();
    }

    @Operation(summary = "获取公开分享列表")
    @GetMapping
    public Result<PageResult<ShareVO>> getPublicShares(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(shareService.getPublicShares(keyword, page, size));
    }

    @Operation(summary = "获取分享详情")
    @GetMapping("/{shareId}")
    public Result<ShareVO> getShareDetail(@PathVariable Long shareId) {
        return Result.success(shareService.getShareDetail(shareId));
    }

    @Operation(summary = "发布评论")
    @PostMapping("/{shareId}/comments")
    public Result<Void> addComment(@PathVariable Long shareId, @RequestBody CommentSaveDTO commentDTO) {
        shareService.addComment(shareId, commentDTO);
        return Result.success();
    }

    @Operation(summary = "获取评论列表")
    @GetMapping("/{shareId}/comments")
    public Result<List<CommentVO>> getComments(@PathVariable Long shareId) {
        return Result.success(shareService.getComments(shareId));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        shareService.deleteComment(commentId);
        return Result.success();
    }
}
