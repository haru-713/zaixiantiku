package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.entity.Class;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.StudentSimpleVO;
import com.example.zaixiantiku.service.ClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/classes")
@RequiredArgsConstructor
@Tag(name = "班级管理", description = "管理员和教师管理班级及学生关联")
public class ClassController {

    private final ClassService classService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建班级")
    public Result<Class> createClass(@RequestBody Class clazz) {
        classService.save(clazz);
        return Result.success(clazz);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "修改班级")
    public Result<Class> updateClass(@PathVariable Long id, @RequestBody Class clazz) {
        clazz.setId(id);
        classService.updateById(clazz);
        return Result.success(clazz);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除班级")
    public Result<Void> deleteClass(@PathVariable Long id) {
        classService.deleteClass(id);
        return Result.success();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "班级列表分页查询")
    public Result<PageResult<Class>> getClassPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return Result.success(classService.getClassPage(page, size, keyword));
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "获取班级学生列表")
    public Result<List<StudentSimpleVO>> getClassStudents(@PathVariable Long id) {
        return Result.success(classService.getClassStudents(id));
    }

    @PostMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "向班级批量添加学生")
    public Result<Void> addStudentsToClass(@PathVariable Long id, @RequestBody List<Long> studentIds) {
        classService.addStudentsToClass(id, studentIds);
        return Result.success();
    }

    @DeleteMapping("/{id}/students/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "从班级移除单个学生")
    public Result<Void> removeStudentFromClass(@PathVariable Long id, @PathVariable Long studentId) {
        classService.removeStudentFromClass(id, studentId);
        return Result.success();
    }
}
