package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.pojo.vo.ClassAnalysisVO;
import com.example.zaixiantiku.pojo.vo.GlobalAnalysisVO;
import com.example.zaixiantiku.pojo.vo.StudentAnalysisVO;
import com.example.zaixiantiku.service.AnalysisService;
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
     * 获取学生选修的课程列表
     */
    @GetMapping("/student/analysis/courses")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<java.util.Map<String, Object>>> getStudentEnrolledCourses() {
        return Result.success(analysisService.getStudentEnrolledCourses());
    }

    /**
     * 班级成绩分析 (教师/管理员)
     */
    @GetMapping("/teacher/analysis/class/{classId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<ClassAnalysisVO> getClassAnalysis(
            @PathVariable Long classId,
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) Long courseId) {
        return Result.success(analysisService.getClassAnalysis(classId, examId, courseId));
    }

    /**
     * 获取教师管辖的班级列表 (教师/管理员)
     */
    @GetMapping("/teacher/analysis/classes")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<List<com.example.zaixiantiku.entity.Class>> getTeacherClasses(
            @RequestParam(required = false) Long courseId) {
        return Result.success(analysisService.getTeacherClasses(courseId));
    }

    /**
     * 获取系统中所有班级列表 (管理员)
     */
    @GetMapping("/admin/analysis/classes")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<com.example.zaixiantiku.entity.Class>> getAllClasses(
            @RequestParam(required = false) Long courseId) {
        return Result.success(analysisService.getAllClasses(courseId));
    }

    /**
     * 获取考试列表
     */
    @GetMapping("/teacher/analysis/exams")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<List<Exam>> getExams(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long courseId) {
        return Result.success(analysisService.getExams(classId, courseId));
    }

    /**
     * 获取指定班级的考试列表 (向后兼容)
     */
    @GetMapping("/teacher/analysis/class/{classId}/exams")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<List<Exam>> getClassExams(@PathVariable Long classId) {
        return Result.success(analysisService.getExams(classId, null));
    }

    /**
     * 全校/课程成绩分析概览 (教师/管理员)
     */
    @GetMapping("/admin/analysis/global")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<GlobalAnalysisVO> getGlobalAnalysis(@RequestParam(required = false) Long courseId) {
        return Result.success(analysisService.getGlobalAnalysis(courseId));
    }

    /**
     * 管理员首页概览统计数据
     */
    @GetMapping("/admin/analysis/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<GlobalAnalysisVO> getAdminDashboardStats() {
        return Result.success(analysisService.getAdminDashboardStats());
    }
}
