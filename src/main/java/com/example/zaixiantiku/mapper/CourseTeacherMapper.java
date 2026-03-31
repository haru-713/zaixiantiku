package com.example.zaixiantiku.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.zaixiantiku.entity.CourseTeacher;
import com.example.zaixiantiku.pojo.vo.CourseTeacherRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseTeacherMapper extends BaseMapper<CourseTeacher> {

    @Select("SELECT teacher_id FROM course_teacher WHERE course_id = #{courseId}")
    List<Long> findTeacherIdsByCourseId(Long courseId);

    @Select("SELECT COALESCE(u.name, u.username) FROM user u JOIN course_teacher ct ON u.id = ct.teacher_id WHERE ct.course_id = #{courseId}")
    List<String> findTeacherNamesByCourseId(Long courseId);

    @Select({
            "<script>",
            "SELECT ct.course_id AS courseId, u.id AS id, COALESCE(u.name, u.username) AS name ",
            "FROM course_teacher ct ",
            "JOIN user u ON u.id = ct.teacher_id ",
            "WHERE ct.course_id IN ",
            "<foreach collection='courseIds' item='cid' open='(' separator=',' close=')'>",
            "#{cid}",
            "</foreach>",
            "</script>"
    })
    List<CourseTeacherRowVO> findTeachersByCourseIds(@Param("courseIds") List<Long> courseIds);

    @Select("SELECT teacher_id FROM course_teacher WHERE course_id = #{courseId} ORDER BY create_time ASC LIMIT 1")
    Long findCreatorTeacherIdByCourseId(Long courseId);
}
