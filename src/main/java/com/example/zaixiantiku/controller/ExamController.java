package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.pojo.dto.ExamSaveDTO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.ExamVO;
import com.example.zaixiantiku.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 考试管理控制器
 */
@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
@Tag(name = "考试管理", description = "创建、修改、取消考试安排")
public class ExamController {

    private final ExamService examService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "分页查询考试列表")
    public Result<PageResult<ExamVO>> getExamPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer status) {
        PageResult<ExamVO> res = examService.getExamPage(page, size, keyword, courseId, status);
        return Result.success(res);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "创建考试安排")
    public Result<ExamVO> createExam(@RequestBody ExamSaveDTO saveDTO) {
        ExamVO vo = examService.createExam(saveDTO);
        return Result.success(vo);
    }

    @PutMapping("/{examId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "修改考试安排")
    public Result<ExamVO> updateExam(@PathVariable Long examId, @RequestBody ExamSaveDTO saveDTO) {
        ExamVO vo = examService.updateExam(examId, saveDTO);
        return Result.success(vo);
    }

    @DeleteMapping("/{examId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "取消考试")
    public Result<Void> cancelExam(@PathVariable Long examId) {
        examService.cancelExam(examId);
        return Result.success(1, "取消成功", null);
    }
}
