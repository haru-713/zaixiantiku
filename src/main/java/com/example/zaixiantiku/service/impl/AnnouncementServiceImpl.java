package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
        announcement.setStatus(1); // 默认显示

        // 如果设置为置顶，强制清除其他所有公告的置顶状态
        if (announcement.getIsTop() != null && announcement.getIsTop() == 1) {
            announcementMapper.update(null, new LambdaUpdateWrapper<Announcement>()
                    .set(Announcement::getIsTop, 0)
                    .eq(Announcement::getIsTop, 1));
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

        // 如果设置为置顶，强制清除其他所有公告的置顶状态
        if (announcement.getIsTop() != null && announcement.getIsTop() == 1) {
            announcementMapper.update(null, new LambdaUpdateWrapper<Announcement>()
                    .set(Announcement::getIsTop, 0)
                    .eq(Announcement::getIsTop, 1));
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
    public PageResult<Announcement> getAnnouncements(Integer isTop, Integer page, Integer size) {
        PageHelper.startPage(page, size);
        LambdaQueryWrapper<Announcement> qw = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1) // 仅查询显示状态
                .orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getCreateTime);

        if (isTop != null) {
            qw.eq(Announcement::getIsTop, isTop);
        }

        List<Announcement> list = announcementMapper.selectList(qw);
        PageInfo<Announcement> pageInfo = new PageInfo<>(list);
        return PageResult.of(pageInfo.getTotal(), list);
    }
}
