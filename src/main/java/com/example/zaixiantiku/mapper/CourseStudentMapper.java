package com.example.zaixiantiku.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.zaixiantiku.entity.CourseStudent;
import com.example.zaixiantiku.pojo.vo.StudentSimpleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseStudentMapper extends BaseMapper<CourseStudent> {

    @Select("SELECT u.id AS id, COALESCE(u.name, u.username) AS name " +
            "FROM course_student cs " +
            "JOIN user u ON u.id = cs.student_id " +
            "WHERE cs.course_id = #{courseId}")
    List<StudentSimpleVO> findStudentsByCourseId(Long courseId);
}
