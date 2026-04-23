package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.pojo.dto.CheatRecordDTO;
import com.example.zaixiantiku.pojo.dto.ExamSubmitDTO;
import com.example.zaixiantiku.pojo.vo.ExamEnterVO;
import com.example.zaixiantiku.pojo.vo.ExamRecordDetailVO;
import com.example.zaixiantiku.pojo.vo.ExamVO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.StudentExamRecordVO;
import com.example.zaixiantiku.service.StudentExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 学生考试端控制器
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "学生考试", description = "学生参加考试相关接口")
public class StudentExamController {

    private final StudentExamService studentExamService;

    @PostMapping("/student/exam/cheat")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "记录作弊行为（如切屏）")
    public Result<Void> recordCheat(@RequestBody CheatRecordDTO cheatDTO) {
        studentExamService.recordCheat(cheatDTO);
        return Result.success();
    }

    @GetMapping("/student/exams")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "学生：获取可参加的考试")
    public Result<PageResult<ExamVO>> getStudentExams(@RequestParam(required = false) Long courseId) {
        PageResult<ExamVO> res = studentExamService.getStudentExams(courseId);
        return Result.success(res);
    }

    @GetMapping("/exams/{examId}/enter")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "进入考试（获取试卷）")
    public Result<ExamEnterVO> enterExam(@PathVariable Long examId) {
        ExamEnterVO vo = studentExamService.enterExam(examId);
        return Result.success(vo);
    }

    @PostMapping("/exams/{examId}/answers/{questionId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "自动保存单题答案")
    public Result<Void> saveAnswer(@PathVariable Long examId, @PathVariable Long questionId,
            @RequestBody Map<String, String> body) {
        studentExamService.saveAnswer(examId, questionId, body.get("answer"));
        return Result.success();
    }

    @PostMapping("/exams/{examId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "提交试卷")
    public Result<Map<String, Object>> submitExam(@PathVariable Long examId, @RequestBody ExamSubmitDTO submitDTO) {
        Map<String, Object> data = studentExamService.submitExam(examId, submitDTO);
        return Result.success(1, "提交成功", data);
    }

    @GetMapping("/student/exam-records")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "学生：考试记录列表")
    public Result<PageResult<StudentExamRecordVO>> getStudentExamRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long courseId) {
        PageResult<StudentExamRecordVO> res = studentExamService.getStudentExamRecords(page, size, courseId);
        return Result.success(res);
    }

    @GetMapping("/student/exam-records/{recordId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "查看考试成绩/试卷")
    public Result<ExamRecordDetailVO> getExamRecordDetail(@PathVariable Long recordId) {
        ExamRecordDetailVO vo = studentExamService.getExamRecordDetail(recordId);
        return Result.success(vo);
    }
}
