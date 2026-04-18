package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.pojo.vo.ClassAnalysisVO;
import com.example.zaixiantiku.pojo.vo.GlobalAnalysisVO;
import com.example.zaixiantiku.pojo.vo.StudentAnalysisVO;
import com.example.zaixiantiku.service.AnalysisService;
import com.example.zaixiantiku.entity.Class;
import com.example.zaixiantiku.entity.Exam;
import com.example.zaixiantiku.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    /**
     * 学生学习报告
     */
    @GetMapping("/student/analysis/report")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<StudentAnalysisVO> getStudentReport(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String timeRange) {
        return Result.success(analysisService.getStudentAnalysisReport(courseId, timeRange));
    }

    /**
     * 班级成绩分析 (教师/管理员)
     */
    @GetMapping("/teacher/analysis/class/{classId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<ClassAnalysisVO> getClassAnalysis(
            @PathVariable Long classId,
            @RequestParam(required = false) Long examId) {
        return Result.success(analysisService.getClassAnalysis(classId, examId));
    }

    /**
     * 获取教师管辖的班级列表 (教师/管理员)
     */
    @GetMapping("/teacher/analysis/classes")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<List<Class>> getTeacherClasses() {
        return Result.success(analysisService.getTeacherClasses());
    }

    /**
     * 获取系统中所有班级列表 (管理员)
     */
    @GetMapping("/admin/analysis/classes")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Class>> getAllClasses() {
        return Result.success(analysisService.getAllClasses());
    }

    /**
     * 获取指定班级的考试列表
     */
    @GetMapping("/teacher/analysis/class/{classId}/exams")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<List<Exam>> getClassExams(@PathVariable Long classId) {
        return Result.success(analysisService.getClassExams(classId));
    }

    /**
     * 全校成绩分析概览 (教师/管理员)
     */
    @GetMapping("/admin/analysis/global")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<GlobalAnalysisVO> getGlobalAnalysis() {
        return Result.success(analysisService.getGlobalAnalysis());
    }
}
