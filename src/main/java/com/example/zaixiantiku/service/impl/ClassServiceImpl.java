package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.zaixiantiku.entity.Class;
import com.example.zaixiantiku.entity.StudentClass;
import com.example.zaixiantiku.entity.User;
import com.example.zaixiantiku.mapper.ClassMapper;
import com.example.zaixiantiku.mapper.StudentClassMapper;
import com.example.zaixiantiku.mapper.UserMapper;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.StudentSimpleVO;
import com.example.zaixiantiku.service.ClassService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassServiceImpl extends ServiceImpl<ClassMapper, Class> implements ClassService {

    private final StudentClassMapper studentClassMapper;
    private final UserMapper userMapper;

    @Override
    public PageResult<Class> getClassPage(Integer page, Integer size, String keyword) {
        PageHelper.startPage(page, size);
        LambdaQueryWrapper<Class> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(Class::getClassName, keyword).or().like(Class::getGrade, keyword);
        }
        qw.orderByDesc(Class::getId);
        List<Class> list = this.list(qw);
        PageInfo<Class> pageInfo = new PageInfo<>(list);
        return PageResult.of(pageInfo.getTotal(), list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStudentsToClass(Long classId, List<Long> studentIds) {
        for (Long studentId : studentIds) {
            // 先检查是否已经在该班级
            Long count = studentClassMapper.selectCount(new LambdaQueryWrapper<StudentClass>()
                    .eq(StudentClass::getClassId, classId)
                    .eq(StudentClass::getStudentId, studentId));
            if (count == 0) {
                StudentClass sc = StudentClass.builder()
                        .classId(classId)
                        .studentId(studentId)
                        .joinTime(LocalDateTime.now())
                        .build();
                studentClassMapper.insert(sc);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeStudentFromClass(Long classId, Long studentId) {
        studentClassMapper.delete(new LambdaQueryWrapper<StudentClass>()
                .eq(StudentClass::getClassId, classId)
                .eq(StudentClass::getStudentId, studentId));
    }

    @Override
    public List<StudentSimpleVO> getClassStudents(Long classId) {
        List<StudentClass> relations = studentClassMapper.selectList(new LambdaQueryWrapper<StudentClass>()
                .eq(StudentClass::getClassId, classId));
        if (relations.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> studentIds = relations.stream().map(StudentClass::getStudentId).collect(Collectors.toList());
        List<User> students = userMapper.selectBatchIds(studentIds);
        return students.stream().map(s -> StudentSimpleVO.builder()
                .id(s.getId())
                .name(s.getName())
                .username(s.getUsername())
                .avatar(s.getAvatar())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClass(Long classId) {
        // 先清空关联
        studentClassMapper.delete(new LambdaQueryWrapper<StudentClass>()
                .eq(StudentClass::getClassId, classId));
        this.removeById(classId);
    }
}
