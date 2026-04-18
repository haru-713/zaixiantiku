package com.example.zaixiantiku.service;

import com.example.zaixiantiku.pojo.dto.ExamMarkDTO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.TeacherExamRecordVO;

import java.util.Map;

/**
 * 教师考试服务接口
 */
public interface TeacherExamService {

    /**
     * 获取待阅卷的考试记录列表
     */
    PageResult<TeacherExamRecordVO> getPendingMarkingRecords(Long courseId, Integer status, Integer page, Integer size);

    /**
     * 阅卷
     */
    Map<String, Object> markExamRecord(Long recordId, ExamMarkDTO markDTO);
}
