package com.example.zaixiantiku.service;

import com.example.zaixiantiku.pojo.vo.ClassAnalysisVO;
import com.example.zaixiantiku.pojo.vo.GlobalAnalysisVO;
import com.example.zaixiantiku.pojo.vo.StudentAnalysisVO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.StudentExamRecordVO;
import com.example.zaixiantiku.pojo.vo.StudentExamRecordDetailVO;

import java.util.List;

public interface AnalysisService {
    /**
     * 获取学生的分析报告
     */
    StudentAnalysisVO getStudentAnalysisReport(Long courseId, String timeRange);

    /**
     * 获取学生考试概况汇总
     */
    StudentAnalysisVO getStudentExamSummary(Long courseId);

    /**
     * 获取学生参加的考试记录列表
     */
    PageResult<StudentExamRecordVO> getStudentExamRecords(Integer page, Integer size, Long courseId);

    /**
     * 获取学生单次考试的详细分析报告
     */
    StudentExamRecordDetailVO getStudentExamDetail(Long recordId);

    /**
     * 获取学生历次考试得分率趋势
     */
    List<StudentAnalysisVO.TrendVO> getStudentExamScoreTrend(Long courseId);

    /**
     * 获取学生选修的课程列表（用于分析筛选）
     */
    java.util.List<java.util.Map<String, Object>> getStudentEnrolledCourses();

    /**
     * 获取教师班级分析
     * 
     * @param classId  班级ID
     * @param examId   考试ID（可选）
     * @param courseId 课程ID（可选，用于全部考试模式下的课程过滤）
     * @return 班级分析数据
     */
    ClassAnalysisVO getClassAnalysis(Long classId, Long examId, Long courseId);

    /**
     * 获取当前教师管辖的班级列表
     * 
     * @param courseId 课程ID（可选，如果提供则返回该课程关联的班级）
     * @return 班级列表
     */
    java.util.List<com.example.zaixiantiku.entity.Class> getTeacherClasses(Long courseId);

    /**
     * 获取管理员可见的所有班级列表
     * 
     * @param courseId 课程ID（可选）
     * @return 班级列表
     */
    java.util.List<com.example.zaixiantiku.entity.Class> getAllClasses(Long courseId);

    /**
     * 获取指定班级或课程关联的考试列表
     * 
     * @param classId  班级ID（可选）
     * @param courseId 课程ID（可选）
     * @return 考试列表
     */
    java.util.List<com.example.zaixiantiku.entity.Exam> getExams(Long classId, Long courseId);

    /**
     * 获取全校/课程成绩分析概览
     * 
     * @param courseId 课程ID（可选）
     * @return 成绩分析概览
     */
    GlobalAnalysisVO getGlobalAnalysis(Long courseId);

    /**
     * 获取管理员首页概览统计数据
     * 
     * @return 统计数据
     */
    GlobalAnalysisVO getAdminDashboardStats();
}
