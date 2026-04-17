package com.example.zaixiantiku.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.zaixiantiku.entity.StudentClass;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生班级关联 Mapper 接口
 */
@Mapper
public interface StudentClassMapper extends BaseMapper<StudentClass> {
}
