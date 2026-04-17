package com.example.zaixiantiku.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.zaixiantiku.entity.ExamClass;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考试班级关联 Mapper 接口
 */
@Mapper
public interface ExamClassMapper extends BaseMapper<ExamClass> {
}
