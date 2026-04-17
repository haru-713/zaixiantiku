package com.example.zaixiantiku.service;

import com.example.zaixiantiku.entity.Share;
import com.example.zaixiantiku.pojo.dto.CommentSaveDTO;
import com.example.zaixiantiku.pojo.dto.ShareAuditDTO;
import com.example.zaixiantiku.pojo.dto.ShareSaveDTO;
import com.example.zaixiantiku.pojo.vo.CommentVO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.ShareVO;

import java.util.List;

/**
 * 分享交流服务接口
 */
public interface ShareService {
    
    /**
     * 发布分享
     */
    Share saveShare(ShareSaveDTO shareDTO);
    
    /**
     * 修改分享
     */
    Share updateShare(Long shareId, ShareSaveDTO shareDTO);
    
    /**
     * 删除分享
     */
    void deleteShare(Long shareId);
    
    /**
     * 分享列表（公开，已发布）
     */
    PageResult<ShareVO> getPublicShares(String keyword, Integer page, Integer size);
    
    /**
     * 管理员：审核分享
     */
    void auditShare(Long shareId, ShareAuditDTO auditDTO);
    
    /**
     * 添加评论
     */
    void addComment(Long shareId, CommentSaveDTO commentDTO);
    
    /**
     * 获取评论列表
     */
    List<CommentVO> getComments(Long shareId);
    
    /**
     * 删除评论
     */
    void deleteComment(Long commentId);

    /**
     * 置顶/取消置顶分享
     */
    void toggleShareTop(Long shareId, Integer isTop);

    /**
     * 置顶/取消置顶评论
     */
    void toggleCommentTop(Long commentId, Integer isTop);

    /**
     * 获取分享详情
     */
    ShareVO getShareDetail(Long shareId);
}
