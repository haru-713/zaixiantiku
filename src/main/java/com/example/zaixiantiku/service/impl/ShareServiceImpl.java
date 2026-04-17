package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.zaixiantiku.entity.Comment;
import com.example.zaixiantiku.entity.Share;
import com.example.zaixiantiku.entity.User;
import com.example.zaixiantiku.mapper.CommentMapper;
import com.example.zaixiantiku.mapper.ShareMapper;
import com.example.zaixiantiku.mapper.UserMapper;
import com.example.zaixiantiku.pojo.dto.CommentSaveDTO;
import com.example.zaixiantiku.pojo.dto.ShareAuditDTO;
import com.example.zaixiantiku.pojo.dto.ShareSaveDTO;
import com.example.zaixiantiku.pojo.vo.CommentVO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.ShareVO;
import com.example.zaixiantiku.security.LoginUser;
import com.example.zaixiantiku.service.ShareService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShareServiceImpl implements ShareService {

    private final ShareMapper shareMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public Share saveShare(ShareSaveDTO shareDTO) {
        LoginUser loginUser = getLoginUser();
        
        // 如果是管理员，直接发布(1)，否则待审核(0)
        boolean isAdmin = loginUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        Share share = Share.builder()
                .userId(loginUser.getUser().getId())
                .title(shareDTO.getTitle())
                .content(shareDTO.getContent())
                .status(isAdmin ? 1 : 0)
                .isTop(0)
                .viewCount(0)
                .likeCount(0)
                .build();
        shareMapper.insert(share);
        return share;
    }

    @Override
    @Transactional
    public Share updateShare(Long shareId, ShareSaveDTO shareDTO) {
        Share share = shareMapper.selectById(shareId);
        if (share == null)
            throw new RuntimeException("分享不存在");

        LoginUser loginUser = getLoginUser();
        if (!share.getUserId().equals(loginUser.getUser().getId())) {
            throw new RuntimeException("没有权限修改该分享");
        }

        share.setTitle(shareDTO.getTitle());
        share.setContent(shareDTO.getContent());
        if (shareDTO.getIsPublic() != null) {
            share.setStatus(shareDTO.getIsPublic() ? 1 : 0);
        }
        shareMapper.updateById(share);
        return share;
    }

    @Override
    @Transactional
    public void deleteShare(Long shareId) {
        Share share = shareMapper.selectById(shareId);
        if (share == null)
            return;

        LoginUser loginUser = getLoginUser();
        boolean isAdmin = loginUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !share.getUserId().equals(loginUser.getUser().getId())) {
            throw new RuntimeException("没有权限删除该分享");
        }

        // 删除关联评论
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getShareId, shareId));
        shareMapper.deleteById(shareId);
    }

    @Override
    public PageResult<ShareVO> getPublicShares(String keyword, Integer page, Integer size) {
        LoginUser loginUser = null;
        try {
            loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception ignored) {
        }

        boolean isAdmin = loginUser != null && loginUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        PageHelper.startPage(page, size);
        LambdaQueryWrapper<Share> qw = new LambdaQueryWrapper<Share>();

        // 置顶排序始终作为第一优先级
        qw.orderByDesc(Share::getIsTop);

        if (isAdmin) {
            // 管理员可以看到所有状态（待审核、已发布、驳回），并优先看待审核的
            qw.orderByAsc(Share::getStatus);
        } else {
            // 普通用户：看到已发布的(1) OR 属于自己的(0,2)
            Long currentUserId = loginUser != null ? loginUser.getUser().getId() : null;
            qw.and(wrapper -> {
                wrapper.eq(Share::getStatus, 1);
                if (currentUserId != null) {
                    wrapper.or(w -> w.eq(Share::getUserId, currentUserId));
                }
            });
        }

        // 最后按时间倒序
        qw.orderByDesc(Share::getCreateTime);

        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.and(wrapper -> wrapper.like(Share::getTitle, keyword).or().like(Share::getContent, keyword));
        }

        List<Share> shares = shareMapper.selectList(qw);
        PageInfo<Share> pageInfo = new PageInfo<>(shares);

        if (shares.isEmpty()) {
            return PageResult.of(pageInfo.getTotal(), new ArrayList<>());
        }

        // 填充用户名
        List<Long> userIds = shares.stream().map(Share::getUserId).distinct().collect(Collectors.toList());
        Map<Long, String> userNameMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        List<ShareVO> voList = shares.stream().map(s -> ShareVO.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .username(userNameMap.getOrDefault(s.getUserId(), "匿名用户"))
                .title(s.getTitle())
                .content(s.getContent())
                .status(s.getStatus())
                .isTop(s.getIsTop())
                .viewCount(s.getViewCount())
                .likeCount(s.getLikeCount())
                .createTime(s.getCreateTime())
                .updateTime(s.getUpdateTime())
                .build()).collect(Collectors.toList());

        return PageResult.of(pageInfo.getTotal(), voList);
    }

    @Override
    @Transactional
    public void auditShare(Long shareId, ShareAuditDTO auditDTO) {
        Share share = shareMapper.selectById(shareId);
        if (share == null)
            throw new RuntimeException("分享不存在");

        share.setStatus(auditDTO.getStatus());
        // 如果有驳回理由可以记录，当前表结构暂无驳回理由字段，仅更新状态
        shareMapper.updateById(share);
    }

    @Override
    @Transactional
    public void addComment(Long shareId, CommentSaveDTO commentDTO) {
        LoginUser loginUser = getLoginUser();
        Comment comment = Comment.builder()
                .shareId(shareId)
                .userId(loginUser.getUser().getId())
                .content(commentDTO.getContent())
                .isTop(0)
                .build();
        commentMapper.insert(comment);
    }

    @Override
    public List<CommentVO> getComments(Long shareId) {
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getShareId, shareId)
                .orderByDesc(Comment::getIsTop)
                .orderByDesc(Comment::getCreateTime));

        if (comments.isEmpty())
            return new ArrayList<>();

        List<Long> userIds = comments.stream().map(Comment::getUserId).distinct().collect(Collectors.toList());
        Map<Long, String> userNameMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        return comments.stream().map(c -> CommentVO.builder()
                .id(c.getId())
                .shareId(c.getShareId())
                .userId(c.getUserId())
                .username(userNameMap.getOrDefault(c.getUserId(), "匿名用户"))
                .content(c.getContent())
                .isTop(c.getIsTop())
                .createTime(c.getCreateTime())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null)
            return;

        LoginUser loginUser = getLoginUser();
        boolean isAdmin = loginUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !comment.getUserId().equals(loginUser.getUser().getId())) {
            throw new RuntimeException("没有权限删除该评论");
        }

        commentMapper.deleteById(commentId);
    }

    @Override
    @Transactional
    public void toggleShareTop(Long shareId, Integer isTop) {
        Share share = shareMapper.selectById(shareId);
        if (share == null)
            throw new RuntimeException("分享不存在");
        
        if (isTop == 1) {
            // 实现唯一置顶：先取消所有现有的置顶
            Share updateOld = new Share();
            updateOld.setIsTop(0);
            shareMapper.update(updateOld, new LambdaQueryWrapper<Share>().eq(Share::getIsTop, 1));
        }
        
        share.setIsTop(isTop);
        shareMapper.updateById(share);
    }

    @Override
    @Transactional
    public void toggleCommentTop(Long commentId, Integer isTop) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null)
            throw new RuntimeException("评论不存在");
            
        if (isTop == 1) {
            // 实现该分享下评论的唯一置顶：先取消该分享下所有现有的置顶评论
            Comment updateOld = new Comment();
            updateOld.setIsTop(0);
            commentMapper.update(updateOld, new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getShareId, comment.getShareId())
                    .eq(Comment::getIsTop, 1));
        }
        
        comment.setIsTop(isTop);
        commentMapper.updateById(comment);
    }

    @Override
    public ShareVO getShareDetail(Long shareId) {
        Share share = shareMapper.selectById(shareId);
        if (share == null)
            throw new RuntimeException("分享不存在");

        LoginUser loginUser = null;
        try {
            loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception ignored) {
        }

        boolean isAdmin = loginUser != null && loginUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // 如果分享未发布且不是作者也不是管理员，则不能查看
        if (share.getStatus() != 1 && !isAdmin) {
            if (loginUser == null || !share.getUserId().equals(loginUser.getUser().getId())) {
                throw new RuntimeException("该分享尚未发布");
            }
        }

        // 增加浏览量
        share.setViewCount(share.getViewCount() + 1);
        shareMapper.updateById(share);

        User user = userMapper.selectById(share.getUserId());
        return ShareVO.builder()
                .id(share.getId())
                .userId(share.getUserId())
                .username(user != null ? user.getName() : "匿名用户")
                .title(share.getTitle())
                .content(share.getContent())
                .status(share.getStatus())
                .isTop(share.getIsTop())
                .viewCount(share.getViewCount())
                .likeCount(share.getLikeCount())
                .createTime(share.getCreateTime())
                .updateTime(share.getUpdateTime())
                .build();
    }

    private LoginUser getLoginUser() {
        return (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
