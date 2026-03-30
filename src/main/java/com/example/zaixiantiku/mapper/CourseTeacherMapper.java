package com.example.zaixiantiku.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.zaixiantiku.entity.CourseTeacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseTeacherMapper extends BaseMapper<CourseTeacher> {

    @Select("SELECT teacher_id FROM course_teacher WHERE course_id = #{courseId}")
    List<Long> findTeacherIdsByCourseId(Long courseId);

    @Select("SELECT u.name FROM user u JOIN course_teacher ct ON u.id = ct.teacher_id WHERE ct.course_id = #{courseId}")
    List<String> findTeacherNamesByCourseId(Long courseId);
}
