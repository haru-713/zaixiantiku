package com.example.zaixiantiku.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.entity.Log;
import com.example.zaixiantiku.entity.User;
import com.example.zaixiantiku.mapper.LogMapper;
import com.example.zaixiantiku.mapper.UserMapper;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作日志控制器
 */
@Tag(name = "操作日志")
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LogController {

    private final LogMapper logMapper;
    private final UserMapper userMapper;

    @Operation(summary = "获取日志列表")
    @GetMapping
    public Result<PageResult<LogVO>> getLogs(
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        PageHelper.startPage(page, size);
        LambdaQueryWrapper<Log> qw = new LambdaQueryWrapper<Log>()
                .orderByDesc(Log::getCreateTime);
        
        if (module != null && !module.trim().isEmpty()) {
            qw.like(Log::getModule, module);
        }

        List<Log> list = logMapper.selectList(qw);
        PageInfo<Log> pageInfo = new PageInfo<>(list);

        // 填充用户名
        List<Long> userIds = list.stream().map(Log::getUserId).filter(id -> id != null).distinct().collect(Collectors.toList());
        Map<Long, String> userNameMap = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        List<LogVO> vos = list.stream().map(log -> LogVO.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .username(userNameMap.getOrDefault(log.getUserId(), "未知"))
                .operation(log.getOperation())
                .module(log.getModule())
                .params(log.getParams())
                .ip(log.getIp())
                .createTime(log.getCreateTime())
                .build()).collect(Collectors.toList());

        return Result.success(PageResult.of(pageInfo.getTotal(), vos));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogVO {
        private Long id;
        private Long userId;
        private String username;
        private String operation;
        private String module;
        private String params;
        private String ip;
        private LocalDateTime createTime;
    }
}
