package com.example.zaixiantiku.service;

import com.example.zaixiantiku.pojo.vo.ClassAnalysisVO;
import com.example.zaixiantiku.pojo.vo.GlobalAnalysisVO;
import com.example.zaixiantiku.pojo.vo.StudentAnalysisVO;

public interface AnalysisService {
    /**
     * 获取学生学习报告
     * 
     * @param courseId  课程ID（可选）
     * @param timeRange 时间范围（可选，如：week, month, all）
     * @return 学习报告数据
     */
    StudentAnalysisVO getStudentAnalysisReport(Long courseId, String timeRange);

    /**
     * 获取教师班级分析
     * 
     * @param classId 班级ID
     * @param examId  考试ID（可选）
     * @return 班级分析数据
     */
    ClassAnalysisVO getClassAnalysis(Long classId, Long examId);

    /**
     * 获取当前教师管辖的班级列表
     * 
     * @return 班级列表
     */
    java.util.List<com.example.zaixiantiku.entity.Class> getTeacherClasses();

    /**
     * 获取管理员可见的所有班级列表
     * 
     * @return 班级列表
     */
    java.util.List<com.example.zaixiantiku.entity.Class> getAllClasses();

    /**
     * 获取指定班级关联的考试列表
     * 
     * @param classId 班级ID
     * @return 考试列表
     */
    java.util.List<com.example.zaixiantiku.entity.Exam> getClassExams(Long classId);

    /**
     * 获取管理员全校成绩分析概览
     * 
     * @return 全校分析数据
     */
    GlobalAnalysisVO getGlobalAnalysis();
}
