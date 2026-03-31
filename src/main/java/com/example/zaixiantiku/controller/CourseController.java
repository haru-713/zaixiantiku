package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.entity.Course;
import com.example.zaixiantiku.pojo.dto.CourseCreateDTO;
import com.example.zaixiantiku.pojo.dto.CourseQueryDTO;
import com.example.zaixiantiku.pojo.dto.CourseUpdateDTO;
import com.example.zaixiantiku.pojo.vo.CourseListVO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
