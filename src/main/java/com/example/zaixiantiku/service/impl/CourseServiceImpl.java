package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.zaixiantiku.entity.Course;
import com.example.zaixiantiku.entity.CourseStudent;
import com.example.zaixiantiku.entity.CourseTeacher;
import com.example.zaixiantiku.mapper.CourseMapper;
import com.example.zaixiantiku.mapper.CourseStudentMapper;
import com.example.zaixiantiku.mapper.CourseTeacherMapper;
import com.example.zaixiantiku.pojo.dto.CourseAuditDTO;
import com.example.zaixiantiku.pojo.dto.CourseCreateDTO;
import com.example.zaixiantiku.pojo.dto.CourseQueryDTO;
import com.example.zaixiantiku.pojo.dto.CourseStatusDTO;
import com.example.zaixiantiku.pojo.dto.CourseStudentAddDTO;
import com.example.zaixiantiku.pojo.dto.CourseUpdateDTO;
import com.example.zaixiantiku.pojo.vo.CourseAdminVO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.security.LoginUser;
import com.example.zaixiantiku.service.CourseService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseTeacherMapper courseTeacherMapper;
    private final CourseStudentMapper courseStudentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Course createCourse(CourseCreateDTO createDTO) {
        if (createDTO == null) {
            throw new RuntimeException("请求体不能为空");
        }
        if (!StringUtils.hasText(createDTO.getCourseName())) {
            throw new RuntimeException("courseName 不能为空");
        }

        String courseName = createDTO.getCourseName().strip();
        Long nameCount = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                .apply("TRIM(course_name) = {0}", courseName));
        if (nameCount != null && nameCount > 0) {
            throw new RuntimeException("课程名称已存在");
        }

        Course course = Course.builder()
                .courseName(courseName)
                .description(createDTO.getDescription())
                .cover(createDTO.getCover())
                .status(1)
                .auditStatus(0)
                .build();

        try {
            int rows = courseMapper.insert(course);
            if (rows != 1) {
                throw new RuntimeException("创建课程失败");
            }
        } catch (DuplicateKeyException e) {
            throw new RuntimeException("课程名称已存在");
        }

        LoginUser loginUser = getLoginUser();
        if (loginUser != null && loginUser.getRoleCodes() != null && loginUser.getRoleCodes().contains("TEACHER")) {
            CourseTeacher courseTeacher = CourseTeacher.builder()
                    .courseId(course.getId())
                    .teacherId(loginUser.getUser().getId())
                    .build();
            courseTeacherMapper.insert(courseTeacher);
        }

        return courseMapper.selectById(course.getId());
    }

    @Override
    public PageResult<Course> getMyCourses(CourseQueryDTO queryDTO) {
        LoginUser loginUser = requireLoginUser();
        Long userId = loginUser.getUser().getId();
        List<String> roles = loginUser.getRoleCodes();

        Integer page = queryDTO == null || queryDTO.getPage() == null || queryDTO.getPage() < 1 ? 1
                : queryDTO.getPage();
        Integer size = queryDTO == null || queryDTO.getSize() == null || queryDTO.getSize() < 1 ? 10
                : queryDTO.getSize();
        PageHelper.startPage(page, size);

        LambdaQueryWrapper<Course> qw = buildCourseQueryWrapper(queryDTO);

        if (roles != null && roles.contains("ADMIN")) {
            qw.orderByAsc(Course::getId);
            List<Course> list = courseMapper.selectList(qw);
            PageInfo<Course> pageInfo = new PageInfo<>(list);
            return PageResult.of(pageInfo.getTotal(), list);
        }

        if (roles != null && roles.contains("TEACHER")) {
            qw.apply("id IN (SELECT course_id FROM course_teacher WHERE teacher_id = {0})", userId);
        } else {
            qw.apply("id IN (SELECT course_id FROM course_student WHERE student_id = {0})", userId);
        }

        qw.orderByAsc(Course::getId);
        List<Course> list = courseMapper.selectList(qw);
        PageInfo<Course> pageInfo = new PageInfo<>(list);
        return PageResult.of(pageInfo.getTotal(), list);
    }

    @Override
    public PageResult<CourseAdminVO> getAdminCoursePage(CourseQueryDTO queryDTO) {
        Integer page = queryDTO == null || queryDTO.getPage() == null || queryDTO.getPage() < 1 ? 1
                : queryDTO.getPage();
        Integer size = queryDTO == null || queryDTO.getSize() == null || queryDTO.getSize() < 1 ? 10
                : queryDTO.getSize();
        PageHelper.startPage(page, size);

        LambdaQueryWrapper<Course> qw = buildCourseQueryWrapper(queryDTO);
        qw.orderByAsc(Course::getId);

        List<Course> courses = courseMapper.selectList(qw);
        PageInfo<Course> pageInfo = new PageInfo<>(courses);

        List<CourseAdminVO> vos = courses.stream().map(course -> {
            List<String> names = courseTeacherMapper.findTeacherNamesByCourseId(course.getId());
            String teacherNames = "";
            if (names != null && !names.isEmpty()) {
                StringJoiner sj = new StringJoiner(",");
                for (String name : names) {
                    if (StringUtils.hasText(name)) {
                        sj.add(name);
                    }
                }
                teacherNames = sj.toString();
            }
            return CourseAdminVO.builder()
                    .id(course.getId())
                    .courseName(course.getCourseName())
                    .description(course.getDescription())
                    .cover(course.getCover())
                    .status(course.getStatus())
                    .auditStatus(course.getAuditStatus())
                    .auditReason(course.getAuditReason())
                    .createTime(course.getCreateTime())
                    .updateTime(course.getUpdateTime())
                    .teacherNames(teacherNames)
                    .build();
        }).collect(Collectors.toList());

        return PageResult.of(pageInfo.getTotal(), vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditCourse(Long courseId, CourseAuditDTO auditDTO) {
        if (courseId == null) {
            throw new RuntimeException("courseId 不能为空");
        }
        if (auditDTO == null || auditDTO.getAuditStatus() == null) {
            throw new RuntimeException("auditStatus 不能为空");
        }
        if (!Objects.equals(auditDTO.getAuditStatus(), 1) && !Objects.equals(auditDTO.getAuditStatus(), 2)) {
            throw new RuntimeException("auditStatus 参数非法");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        Course update = new Course();
        update.setId(courseId);
        update.setAuditStatus(auditDTO.getAuditStatus());
        update.setAuditReason(auditDTO.getReason());
        int rows = courseMapper.updateById(update);
        if (rows != 1) {
            throw new RuntimeException("审核失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCourseStatus(Long courseId, CourseStatusDTO statusDTO) {
        if (courseId == null) {
            throw new RuntimeException("courseId 不能为空");
        }
        if (statusDTO == null || statusDTO.getStatus() == null) {
            throw new RuntimeException("status 不能为空");
        }
        if (!Objects.equals(statusDTO.getStatus(), 0) && !Objects.equals(statusDTO.getStatus(), 1)) {
            throw new RuntimeException("status 参数非法");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        Course update = new Course();
        update.setId(courseId);
        update.setStatus(statusDTO.getStatus());
        int rows = courseMapper.updateById(update);
        if (rows != 1) {
            throw new RuntimeException("状态更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Course updateCourse(Long courseId, CourseUpdateDTO updateDTO) {
        if (courseId == null) {
            throw new RuntimeException("courseId 不能为空");
        }
        if (updateDTO == null) {
            throw new RuntimeException("请求体不能为空");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        requireCourseOwnerOrAdmin(courseId);

        Course update = new Course();
        update.setId(courseId);
        if (StringUtils.hasText(updateDTO.getCourseName())) {
            String courseName = updateDTO.getCourseName().strip();
            Long nameCount = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                    .apply("TRIM(course_name) = {0}", courseName)
                    .ne(Course::getId, courseId));
            if (nameCount != null && nameCount > 0) {
                throw new RuntimeException("课程名称已存在");
            }
            update.setCourseName(courseName);
        }
        if (updateDTO.getDescription() != null) {
            update.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getCover() != null) {
            update.setCover(updateDTO.getCover());
        }

        int rows = courseMapper.updateById(update);
        if (rows != 1) {
            throw new RuntimeException("更新课程失败");
        }
        return courseMapper.selectById(courseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourse(Long courseId) {
        if (courseId == null) {
            throw new RuntimeException("courseId 不能为空");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        requireCourseOwnerOrAdmin(courseId);

        int rows = courseMapper.deleteById(courseId);
        if (rows != 1) {
            throw new RuntimeException("删除课程失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStudents(Long courseId, CourseStudentAddDTO addDTO) {
        if (courseId == null) {
            throw new RuntimeException("courseId 不能为空");
        }
        if (addDTO == null || addDTO.getStudentIds() == null || addDTO.getStudentIds().isEmpty()) {
            throw new RuntimeException("studentIds 不能为空");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        for (Long studentId : addDTO.getStudentIds()) {
            if (studentId == null) {
                continue;
            }
            Long count = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, courseId)
                    .eq(CourseStudent::getStudentId, studentId));
            if (count != null && count > 0) {
                continue;
            }
            courseStudentMapper.insert(CourseStudent.builder()
                    .courseId(courseId)
                    .studentId(studentId)
                    .build());
        }
    }

    private static LambdaQueryWrapper<Course> buildCourseQueryWrapper(CourseQueryDTO queryDTO) {
        LambdaQueryWrapper<Course> qw = new LambdaQueryWrapper<>();
        if (queryDTO == null) {
            return qw;
        }
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            qw.like(Course::getCourseName, queryDTO.getKeyword());
        }
        if (queryDTO.getStatus() != null) {
            qw.eq(Course::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getAuditStatus() != null) {
            qw.eq(Course::getAuditStatus, queryDTO.getAuditStatus());
        }
        return qw;
    }

    private static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    private static LoginUser requireLoginUser() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null || loginUser.getUser() == null || loginUser.getUser().getId() == null) {
            throw new RuntimeException("用户未登录");
        }
        return loginUser;
    }

    private void requireCourseOwnerOrAdmin(Long courseId) {
        LoginUser loginUser = requireLoginUser();
        List<String> roleCodes = loginUser.getRoleCodes();
        if (roleCodes != null && roleCodes.contains("ADMIN")) {
            return;
        }
        if (roleCodes == null || !roleCodes.contains("TEACHER")) {
            throw new RuntimeException("没有权限操作该课程");
        }
        Long teacherId = loginUser.getUser().getId();
        Long count = courseTeacherMapper.selectCount(new LambdaQueryWrapper<CourseTeacher>()
                .eq(CourseTeacher::getCourseId, courseId)
                .eq(CourseTeacher::getTeacherId, teacherId));
        if (count == null || count <= 0) {
            throw new RuntimeException("没有权限操作该课程");
        }
    }
}
