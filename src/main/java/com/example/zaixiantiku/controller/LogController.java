package com.example.zaixiantiku.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.zaixiantiku.common.annotation.OperationLog;
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
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        LambdaQueryWrapper<Log> qw = new LambdaQueryWrapper<Log>()
                .orderByDesc(Log::getCreateTime);

        if (module != null && !module.trim().isEmpty()) {
            qw.like(Log::getModule, module);
        }

        if (username != null && !username.trim().isEmpty()) {
            // 1. 根据用户名或姓名查询对应的用户ID列表
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .like(User::getUsername, username)
                    .or()
                    .like(User::getName, username));

            // 2. 构造查询条件：要么 user_id 在匹配的 ID 列表中，要么 params 字段中包含该用户名
            qw.and(wrapper -> {
                if (!users.isEmpty()) {
                    List<Long> filteredUserIds = users.stream().map(User::getId).collect(Collectors.toList());
                    wrapper.in(Log::getUserId, filteredUserIds);
                }
                // 同时在 params 字段中模糊匹配用户名，以兼容 user_id 为空的历史日志
                wrapper.or().like(Log::getParams, "\"username\":\"" + username);
                // 或者直接模糊匹配 username (针对某些简化的 params 结构)
                wrapper.or().like(Log::getParams, username);
            });
        }

        PageHelper.startPage(page, size);
        List<Log> list = logMapper.selectList(qw);
        PageInfo<Log> pageInfo = new PageInfo<>(list);

        // 填充用户名
        List<Long> userIds = list.stream().map(Log::getUserId).filter(id -> id != null).distinct()
                .collect(Collectors.toList());
        Map<Long, String> userNameMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getName));

        List<LogVO> vos = list.stream().map(log -> {
            String opUsername = "未知";
            if (log.getUserId() != null) {
                opUsername = userNameMap.getOrDefault(log.getUserId(), "未知");
            } else if (log.getParams() != null && log.getParams().contains("\"username\":\"")) {
                // 针对 userId 为空的历史日志，从参数中提取用户名 (用于展示)
                try {
                    String p = log.getParams();
                    int start = p.indexOf("\"username\":\"") + 12;
                    int end = p.indexOf("\"", start);
                    if (start > 11 && end > start) {
                        opUsername = p.substring(start, end);
                    }
                } catch (Exception ignored) {
                }
            }

            String params = log.getParams();
            // 敏感信息脱敏 (针对历史记录)
            if (params != null) {
                params = params.replaceAll("\"password\":\"[^\"]+\"", "\"password\":\"******\"");
            }

            return LogVO.builder()
                    .id(log.getId())
                    .userId(log.getUserId())
                    .username(opUsername)
                    .operation(log.getOperation())
                    .module(log.getModule())
                    .params(params)
                    .ip(log.getIp())
                    .createTime(log.getCreateTime())
                    .build();
        }).collect(Collectors.toList());

        return Result.success(PageResult.of(pageInfo.getTotal(), vos));
    }

    @Operation(summary = "删除日志")
    @DeleteMapping("/{id}")
    @OperationLog(module = "系统日志", operation = "删除单条日志")
    public Result<Void> deleteLog(@PathVariable Long id) {
        logMapper.deleteById(id);
        return Result.success();
    }

    @Operation(summary = "批量删除日志")
    @DeleteMapping("/batch")
    @OperationLog(module = "系统日志", operation = "批量删除日志")
    public Result<Void> batchDeleteLogs(@RequestBody List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            logMapper.deleteBatchIds(ids);
        }
        return Result.success();
    }

    @Operation(summary = "清空日志")
    @DeleteMapping("/clear")
    @OperationLog(module = "系统日志", operation = "清空所有日志")
    public Result<Void> clearLogs() {
        logMapper.delete(null);
        return Result.success();
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
