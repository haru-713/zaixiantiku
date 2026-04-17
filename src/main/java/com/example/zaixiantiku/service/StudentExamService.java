package com.example.zaixiantiku.service;

import com.example.zaixiantiku.pojo.dto.ExamSubmitDTO;
import com.example.zaixiantiku.pojo.vo.ExamEnterVO;
import com.example.zaixiantiku.pojo.vo.ExamVO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.StudentExamRecordVO;

import java.util.Map;

/**
 * 学生考试服务接口
 */
public interface StudentExamService {

    /**
     * 获取学生可参加的考试列表
     */
    PageResult<ExamVO> getStudentExams(Long courseId);

    /**
     * 进入考试，获取试卷内容
     */
    ExamEnterVO enterExam(Long examId);

    /**
     * 提交试卷
     */
    Map<String, Object> submitExam(Long examId, ExamSubmitDTO submitDTO);

    /**
     * 获取学生考试记录列表
     */
    PageResult<StudentExamRecordVO> getStudentExamRecords(Integer page, Integer size);
}
