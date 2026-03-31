package com.example.zaixiantiku.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.zaixiantiku.entity.Course;
import com.example.zaixiantiku.pojo.dto.CourseAuditDTO;
import com.example.zaixiantiku.pojo.dto.CourseCreateDTO;
import com.example.zaixiantiku.pojo.dto.CourseQueryDTO;
import com.example.zaixiantiku.pojo.dto.CourseStatusDTO;
import com.example.zaixiantiku.pojo.dto.CourseTeacherAddDTO;
import com.example.zaixiantiku.pojo.dto.CourseStudentAddDTO;
import com.example.zaixiantiku.pojo.dto.CourseUpdateDTO;
import com.example.zaixiantiku.pojo.vo.CourseAdminVO;
import com.example.zaixiantiku.pojo.vo.CourseDetailVO;
import com.example.zaixiantiku.pojo.vo.CourseListVO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.StudentSimpleVO;
import com.example.zaixiantiku.pojo.vo.TeacherSimpleVO;

public interface CourseService extends IService<Course> {

    Course createCourse(CourseCreateDTO createDTO);

    PageResult<CourseListVO> getCoursePage(CourseQueryDTO queryDTO);

    CourseDetailVO getCourseDetail(Long courseId);

    PageResult<TeacherSimpleVO> getTeacherCandidates(Long courseId, Integer page, Integer size, String keyword);

    void addTeachers(Long courseId, CourseTeacherAddDTO addDTO);

    void removeTeacher(Long courseId, Long teacherId);

    PageResult<StudentSimpleVO> getStudentCandidates(Long courseId, Integer page, Integer size, String keyword);

    void removeStudent(Long courseId, Long studentId);

    PageResult<Course> getMyCourses(CourseQueryDTO queryDTO);

    PageResult<CourseAdminVO> getAdminCoursePage(CourseQueryDTO queryDTO);

    void auditCourse(Long courseId, CourseAuditDTO auditDTO);

    void updateCourseStatus(Long courseId, CourseStatusDTO statusDTO);

    Course updateCourse(Long courseId, CourseUpdateDTO updateDTO);

    void deleteCourse(Long courseId);

    void addStudents(Long courseId, CourseStudentAddDTO addDTO);
}
