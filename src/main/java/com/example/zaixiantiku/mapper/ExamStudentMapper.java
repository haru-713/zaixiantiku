package com.example.zaixiantiku.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.zaixiantiku.entity.ExamStudent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考试学生关联 Mapper 接口
 */
@Mapper
public interface ExamStudentMapper extends BaseMapper<ExamStudent> {
}
