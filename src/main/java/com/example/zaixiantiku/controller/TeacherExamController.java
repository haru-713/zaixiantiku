package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.pojo.dto.ExamMarkDTO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.TeacherExamRecordVO;
import com.example.zaixiantiku.service.TeacherExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "教师阅卷管理", description = "教师阅卷相关接口")
public class TeacherExamController {

    private final TeacherExamService teacherExamService;

    @GetMapping("/teacher/exam-records/pending")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "教师：待阅卷列表")
    public Result<PageResult<TeacherExamRecordVO>> getPendingMarkingRecords(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResult<TeacherExamRecordVO> res = teacherExamService.getPendingMarkingRecords(courseId, status, page, size);
        return Result.success(res);
    }

    @PutMapping("/teacher/exam-records/{recordId}/mark")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "教师：阅卷")
    public Result<Map<String, Object>> markExamRecord(
            @PathVariable Long recordId,
            @RequestBody ExamMarkDTO markDTO) {
        Map<String, Object> data = teacherExamService.markExamRecord(recordId, markDTO);
        return Result.success(1, "批阅成功", data);
    }
}
