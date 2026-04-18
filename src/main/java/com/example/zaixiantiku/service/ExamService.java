package com.example.zaixiantiku.service;

import com.example.zaixiantiku.pojo.dto.ExamSaveDTO;
import com.example.zaixiantiku.pojo.vo.ExamVO;

/**
 * 考试服务接口
 */
public interface ExamService {

    /**
     * 创建考试
     */
    ExamVO createExam(ExamSaveDTO saveDTO);

    /**
     * 修改考试
     */
    ExamVO updateExam(Long examId, ExamSaveDTO saveDTO);

    /**
     * 取消考试
     */
    void cancelExam(Long examId);

    /**
     * 分页查询考试列表
     */
    com.example.zaixiantiku.pojo.vo.PageResult<ExamVO> getExamPage(Integer page, Integer size, String keyword,
            Long courseId, Long classId, Integer status);
}
