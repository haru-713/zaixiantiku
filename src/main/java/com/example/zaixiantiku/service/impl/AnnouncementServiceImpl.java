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

        announcement.setId(id);
        announcementMapper.updateById(announcement);
        return announcement;
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
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<Announcement> qw = new LambdaQueryWrapper<Announcement>();

        if (filterExpired) {
            qw.eq(Announcement::getStatus, 1); // 仅查询显示状态

            // 过滤未开始的
            qw.and(wrapper -> wrapper
                    .le(Announcement::getStartTime, now)
                    .or()
                    .isNull(Announcement::getStartTime));

            // 过滤已过期的
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
        PageInfo<Announcement> pageInfo = new PageInfo<>(list);
        return PageResult.of(pageInfo.getTotal(), list);
    }
}
