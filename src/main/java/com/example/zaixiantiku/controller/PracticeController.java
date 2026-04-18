package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.entity.MistakeBook;
import com.example.zaixiantiku.pojo.dto.PracticeStartDTO;
import com.example.zaixiantiku.pojo.dto.PracticeSubmitDTO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.PracticeRecordVO;
import com.example.zaixiantiku.pojo.vo.QuestionDetailVO;
import com.example.zaixiantiku.service.PracticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "练习与错题本", description = "学生练习相关接口")
public class PracticeController {

    private final PracticeService practiceService;

    @PostMapping("/practice/start")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "开始练习")
    public Result<Map<String, Object>> startPractice(@RequestBody PracticeStartDTO startDTO) {
        return Result.success(practiceService.startPractice(startDTO));
    }

    @PostMapping("/practice/{practiceId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "提交练习")
    public Result<Map<String, Object>> submitPractice(@PathVariable Long practiceId,
            @RequestBody PracticeSubmitDTO submitDTO) {
        return Result.success(1, "练习完成", practiceService.submitPractice(practiceId, submitDTO));
    }

    @GetMapping("/student/practice-records")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "练习记录列表")
    public Result<PageResult<PracticeRecordVO>> getPracticeRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order) {
        return Result.success(practiceService.getPracticeRecords(page, size, courseId, sortBy, order));
    }

    @GetMapping("/student/practice-report/{practiceId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "获取练习报告详情")
    public Result<Map<String, Object>> getPracticeReport(@PathVariable Long practiceId) {
        return Result.success(practiceService.getPracticeReport(practiceId));
    }

    @DeleteMapping("/student/practice-records/{practiceId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "删除练习记录")
    public Result<Void> removePracticeRecord(@PathVariable Long practiceId) {
        practiceService.removePracticeRecord(practiceId);
        return Result.success(1, "删除成功", null);
    }

    @GetMapping("/student/mistake-book")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "错题本列表")
    public Result<List<Map<String, Object>>> getMistakeBook(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer typeId) {
        return Result.success(practiceService.getMistakeBook(courseId, typeId));
    }

    @PostMapping("/student/mistake-book/{mistakeId}/redo")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "获取重选题详情")
    public Result<Map<String, Object>> redoMistake(@PathVariable Long mistakeId) {
        return Result.success(practiceService.redoMistake(mistakeId));
    }

    @PostMapping("/student/mistake-book/{mistakeId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "提交重做结果")
    public Result<Boolean> submitRedo(@PathVariable Long mistakeId, @RequestBody Map<String, String> body) {
        return Result.success(practiceService.submitRedo(mistakeId, body.get("answer")));
    }

    @DeleteMapping("/student/mistake-book/{mistakeId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "移出错题")
    public Result<Void> removeMistake(@PathVariable Long mistakeId) {
        practiceService.removeMistake(mistakeId);
        return Result.success(1, "移出错题本", null);
    }

    @PutMapping("/student/mistake-book/{mistakeId}/note")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "添加错题笔记")
    public Result<MistakeBook> updateMistakeNote(@PathVariable Long mistakeId, @RequestBody Map<String, String> body) {
        return Result.success(practiceService.updateMistakeNote(mistakeId, body.get("note")));
    }

    @PostMapping("/student/favorites")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "收藏题目")
    public Result<Void> addFavorite(@RequestBody Map<String, Long> body) {
        practiceService.addFavorite(body.get("questionId"));
        return Result.success(1, "收藏成功", null);
    }

    @DeleteMapping("/student/favorites/{favoriteId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "取消收藏")
    public Result<Void> removeFavorite(@PathVariable Long favoriteId) {
        practiceService.removeFavorite(favoriteId);
        return Result.success(1, "取消收藏", null);
    }

    @GetMapping("/student/favorites")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "收藏列表")
    public Result<PageResult<QuestionDetailVO>> getFavorites(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long kpId) {
        return Result.success(practiceService.getFavorites(page, size, courseId, kpId));
    }
}
