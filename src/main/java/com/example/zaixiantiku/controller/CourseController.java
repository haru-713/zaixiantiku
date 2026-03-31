package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.entity.Course;
import com.example.zaixiantiku.pojo.dto.CourseCreateDTO;
import com.example.zaixiantiku.pojo.dto.CourseQueryDTO;
import com.example.zaixiantiku.pojo.dto.CourseStudentAddDTO;
import com.example.zaixiantiku.pojo.dto.CourseTeacherAddDTO;
import com.example.zaixiantiku.pojo.dto.CourseUpdateDTO;
import com.example.zaixiantiku.pojo.vo.CourseDetailVO;
import com.example.zaixiantiku.pojo.vo.CourseListVO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.StudentSimpleVO;
import com.example.zaixiantiku.pojo.vo.TeacherSimpleVO;
import com.example.zaixiantiku.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "课程管理", description = "课程相关接口")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "创建课程", description = "创建新课程")
    public Result<Course> createCourse(@RequestBody CourseCreateDTO createDTO) {
        Course course = courseService.createCourse(createDTO);
        return Result.success(course);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @Operation(summary = "课程列表 (分页)", description = "支持按课程名/状态/教师/学生过滤，返回课程及教师列表")
    public Result<PageResult<CourseListVO>> getCoursePage(CourseQueryDTO queryDTO) {
        PageResult<CourseListVO> pageResult = courseService.getCoursePage(queryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @Operation(summary = "获取课程详情", description = "课程相关人员或管理员可查看")
    public Result<CourseDetailVO> getCourseDetail(@PathVariable Long courseId) {
        CourseDetailVO detail = courseService.getCourseDetail(courseId);
        return Result.success(detail);
    }

    @GetMapping("/{courseId}/teachers/candidates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "课程教师候选列表 (分页)", description = "用于课程教师管理选择组件，支持分页与关键字搜索")
    public Result<PageResult<TeacherSimpleVO>> getTeacherCandidates(
            @PathVariable Long courseId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword) {
        PageResult<TeacherSimpleVO> result = courseService.getTeacherCandidates(courseId, page, size, keyword);
        return Result.success(result);
    }

    @PostMapping("/{courseId}/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "课程教师管理-添加", description = "仅管理员为课程添加教师")
    public Result<Void> addTeachers(@PathVariable Long courseId, @RequestBody CourseTeacherAddDTO addDTO) {
        courseService.addTeachers(courseId, addDTO);
        return Result.success(null);
    }

    @DeleteMapping("/{courseId}/teachers")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "课程教师管理-移除", description = "管理员或课程创建者从课程移除教师")
    public Result<Void> removeTeacher(@PathVariable Long courseId, @RequestParam Long teacherId) {
        courseService.removeTeacher(courseId, teacherId);
        return Result.success(null);
    }

    @GetMapping("/{courseId}/students/candidates")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "课程学生候选列表 (分页)", description = "用于课程学生管理选择组件，支持分页与关键字搜索")
    public Result<PageResult<StudentSimpleVO>> getStudentCandidates(
            @PathVariable Long courseId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword) {
        PageResult<StudentSimpleVO> result = courseService.getStudentCandidates(courseId, page, size, keyword);
        return Result.success(result);
    }

    @PostMapping("/{courseId}/students")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "课程学生管理-添加", description = "课程教师或管理员为课程添加学生")
    public Result<Void> addStudents(@PathVariable Long courseId, @RequestBody CourseStudentAddDTO addDTO) {
        courseService.addStudents(courseId, addDTO);
        return Result.success(null);
    }

    @DeleteMapping("/{courseId}/students")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "课程学生管理-移除", description = "课程教师或管理员从课程移除学生")
    public Result<Void> removeStudent(@PathVariable Long courseId, @RequestParam Long studentId) {
        courseService.removeStudent(courseId, studentId);
        return Result.success(null);
    }

    @PutMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "修改课程", description = "根据 courseId 修改课程信息")
    public Result<Course> updateCourse(@PathVariable Long courseId, @RequestBody CourseUpdateDTO updateDTO) {
        Course course = courseService.updateCourse(courseId, updateDTO);
        return Result.success(course);
    }

    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "删除课程", description = "根据 courseId 删除课程")
    public Result<Void> deleteCourse(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return Result.success(null);
    }
}
