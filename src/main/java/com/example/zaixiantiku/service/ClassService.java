package com.example.zaixiantiku.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.zaixiantiku.entity.Class;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.StudentSimpleVO;
import java.util.List;

public interface ClassService extends IService<Class> {
    PageResult<Class> getClassPage(Integer page, Integer size, String keyword);
    void addStudentsToClass(Long classId, List<Long> studentIds);
    void removeStudentFromClass(Long classId, Long studentId);
    List<StudentSimpleVO> getClassStudents(Long classId);
    void deleteClass(Long classId);
    List<Class> getClassesByCourse(Long courseId);
}
