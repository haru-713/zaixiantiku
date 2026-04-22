package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.zaixiantiku.entity.Announcement;
import com.example.zaixiantiku.mapper.AnnouncementMapper;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.security.LoginUser;
import com.example.zaixiantiku.service.AnnouncementService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    @Override
    @Transactional
    public Announcement saveAnnouncement(Announcement announcement) {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        announcement.setCreateBy(loginUser.getUser().getId());

        if (announcement.getStatus() == null) {
            announcement.setStatus(1); // 默认显示
        }

        // 校验：如果设为显示，且设置了结束时间，则结束时间不能早于当前时间
        if (announcement.getStatus() == 1 && announcement.getEndTime() != null
                && announcement.getEndTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("该公告已过期，如需重新显示请修改结束时间");
        }

        // 唯一置顶逻辑：如果当前公告设为置顶，则取消其他所有置顶
        if (announcement.getIsTop() != null && announcement.getIsTop() == 1) {
            unpinAllAnnouncements();
        }

        announcementMapper.insert(announcement);
        return announcement;
    }

    @Override
    @Transactional
    public Announcement updateAnnouncement(Long id, Announcement announcement) {
        Announcement exist = announcementMapper.selectById(id);
        if (exist == null)
            throw new RuntimeException("公告不存在");

        // 校验：如果设为显示，且设置了结束时间，则结束时间不能早于当前时间
        if (announcement.getStatus() != null && announcement.getStatus() == 1) {
            LocalDateTime endTime = announcement.getEndTime() != null ? announcement.getEndTime() : exist.getEndTime();
            if (endTime != null && endTime.isBefore(LocalDateTime.now())) {
                throw new RuntimeException("该公告已过期，如需重新显示请修改结束时间");
            }
        }

        // 唯一置顶逻辑：如果当前公告被修改为置顶，则取消其他所有置顶
        if (announcement.getIsTop() != null && announcement.getIsTop() == 1) {
            unpinAllAnnouncements();
        }

        announcement.setId(id);
        announcementMapper.updateById(announcement);
        return announcement;
    }

    private void unpinAllAnnouncements() {
        Announcement update = new Announcement();
        update.setIsTop(0);
        announcementMapper.update(update, new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getIsTop, 1));
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long id) {
        announcementMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteExpiredAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        announcementMapper.delete(new LambdaQueryWrapper<Announcement>()
                .isNotNull(Announcement::getEndTime)
                .lt(Announcement::getEndTime, now));
    }

    @Override
    public PageResult<Announcement> getAnnouncements(Integer isTop, Integer page, Integer size, boolean filterExpired) {
        PageHelper.startPage(page, size);

        LambdaQueryWrapper<Announcement> qw = new LambdaQueryWrapper<Announcement>();

        if (filterExpired) {
            // 面向普通用户时：
            // 1. 必须是“显示”状态
            qw.eq(Announcement::getStatus, 1);

            // 2. 必须未过期（或未设置结束时间）
            // 不再过滤“未开始”的公告，只要管理员设为“显示”，用户即可看到（如提前发布的通知）
            LocalDateTime now = LocalDateTime.now();
            qw.and(wrapper -> wrapper
                    .ge(Announcement::getEndTime, now)
                    .or()
                    .isNull(Announcement::getEndTime));
        }

        if (isTop != null) {
            qw.eq(Announcement::getIsTop, isTop);
        }

        // 排序逻辑：置顶优先，其次按创建时间倒序
        qw.orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getCreateTime);

        List<Announcement> list = announcementMapper.selectList(qw);

        // 业务逻辑优化：如果是面向普通用户的视图，且数据库中存在多条置顶（历史遗留数据），
        // 则强制只将最新的一条保留置顶标识，其余在展示层临时改为非置顶，确保 UI 的“唯一置顶”感。
        if (filterExpired && !list.isEmpty()) {
            boolean foundTop = false;
            for (Announcement a : list) {
                if (a.getIsTop() != null && a.getIsTop() == 1) {
                    if (foundTop) {
                        a.setIsTop(0); // 后续的置顶在展示时降级为普通公告
                    } else {
                        foundTop = true;
                    }
                }
            }
        }

        PageInfo<Announcement> pageInfo = new PageInfo<>(list);
        return PageResult.of(pageInfo.getTotal(), list);
    }
}
