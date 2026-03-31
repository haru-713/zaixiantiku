package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.zaixiantiku.entity.Course;
import com.example.zaixiantiku.entity.CourseStudent;
import com.example.zaixiantiku.entity.CourseTeacher;
import com.example.zaixiantiku.entity.User;
import com.example.zaixiantiku.mapper.CourseMapper;
import com.example.zaixiantiku.mapper.CourseStudentMapper;
import com.example.zaixiantiku.mapper.CourseTeacherMapper;
import com.example.zaixiantiku.mapper.UserMapper;
import com.example.zaixiantiku.pojo.dto.CourseAuditDTO;
import com.example.zaixiantiku.pojo.dto.CourseCreateDTO;
import com.example.zaixiantiku.pojo.dto.CourseQueryDTO;
import com.example.zaixiantiku.pojo.dto.CourseStatusDTO;
import com.example.zaixiantiku.pojo.dto.CourseStudentAddDTO;
import com.example.zaixiantiku.pojo.dto.CourseTeacherAddDTO;
import com.example.zaixiantiku.pojo.dto.CourseUpdateDTO;
import com.example.zaixiantiku.pojo.vo.CourseAdminVO;
import com.example.zaixiantiku.pojo.vo.CourseDetailVO;
import com.example.zaixiantiku.pojo.vo.CourseListVO;
import com.example.zaixiantiku.pojo.vo.CourseTeacherRowVO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.StudentSimpleVO;
import com.example.zaixiantiku.pojo.vo.TeacherSimpleVO;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseTeacherMapper courseTeacherMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final UserMapper userMapper;

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
                .auditStatus(1)
                .auditReason(null)
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
    public PageResult<CourseListVO> getCoursePage(CourseQueryDTO queryDTO) {
        LoginUser loginUser = requireLoginUser();
        Long userId = loginUser.getUser().getId();
        List<String> roles = loginUser.getRoleCodes();

        Integer page = queryDTO == null || queryDTO.getPage() == null || queryDTO.getPage() < 1 ? 1
                : queryDTO.getPage();
        Integer size = queryDTO == null || queryDTO.getSize() == null || queryDTO.getSize() < 1 ? 10
                : queryDTO.getSize();
        PageHelper.startPage(page, size);

        Long teacherId = queryDTO == null ? null : queryDTO.getTeacherId();
        Long studentId = queryDTO == null ? null : queryDTO.getStudentId();

        if (roles != null && roles.contains("TEACHER") && (roles.contains("ADMIN") == false)) {
            teacherId = userId;
            studentId = null;
        } else if (roles == null || roles.contains("ADMIN") == false) {
            if (roles != null && roles.contains("STUDENT")) {
                studentId = userId;
                teacherId = null;
            }
        }

        LambdaQueryWrapper<Course> qw = buildCourseQueryWrapper(queryDTO);
        if (teacherId != null) {
            qw.apply("id IN (SELECT course_id FROM course_teacher WHERE teacher_id = {0})", teacherId);
        }
        if (studentId != null) {
            qw.apply("id IN (SELECT course_id FROM course_student WHERE student_id = {0})", studentId);
        }
        qw.orderByAsc(Course::getId);

        List<Course> courses = courseMapper.selectList(qw);
        PageInfo<Course> pageInfo = new PageInfo<>(courses);

        if (courses == null || courses.isEmpty()) {
            return PageResult.of(pageInfo.getTotal(), Collections.emptyList());
        }

        List<Long> courseIds = courses.stream().map(Course::getId).filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, List<TeacherSimpleVO>> teachersByCourseId = new HashMap<>();
        if (!courseIds.isEmpty()) {
            List<CourseTeacherRowVO> rows = courseTeacherMapper.findTeachersByCourseIds(courseIds);
            if (rows != null && !rows.isEmpty()) {
                Map<Long, List<TeacherSimpleVO>> tmp = rows.stream().filter(r -> r.getCourseId() != null)
                        .collect(Collectors.groupingBy(
                                CourseTeacherRowVO::getCourseId,
                                Collectors.mapping(
                                        r -> TeacherSimpleVO.builder().id(r.getId()).name(r.getName()).build(),
                                        Collectors.toList())));
                teachersByCourseId.putAll(tmp);
            }
        }

        List<CourseListVO> list = courses.stream().map(course -> CourseListVO.builder()
                .id(course.getId())
                .courseName(course.getCourseName())
                .description(course.getDescription())
                .cover(course.getCover())
                .status(course.getStatus())
                .teachers(teachersByCourseId.getOrDefault(course.getId(), Collections.emptyList()))
                .createTime(course.getCreateTime())
                .updateTime(course.getUpdateTime())
                .build()).collect(Collectors.toList());

        return PageResult.of(pageInfo.getTotal(), list);
    }

    @Override
    public CourseDetailVO getCourseDetail(Long courseId) {
        if (courseId == null) {
            throw new RuntimeException("courseId 不能为空");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        LoginUser loginUser = requireLoginUser();
        requireCourseMemberOrAdmin(courseId);

        List<TeacherSimpleVO> teachers = Collections.emptyList();
        if (isAdmin(loginUser) || isCourseTeacher(loginUser, courseId)) {
            List<CourseTeacherRowVO> teacherRows = courseTeacherMapper.findTeachersByCourseIds(List.of(courseId));
            teachers = teacherRows == null ? Collections.emptyList()
                    : teacherRows.stream()
                            .map(r -> TeacherSimpleVO.builder().id(r.getId()).name(r.getName()).build())
                            .collect(Collectors.toList());
        }

        List<StudentSimpleVO> students = courseStudentMapper.findStudentsByCourseId(courseId);
        if (students == null) {
            students = Collections.emptyList();
        }

        boolean canAddTeacher = isAdmin(loginUser);
        boolean canRemoveTeacher = isAdmin(loginUser) || isCourseCreator(loginUser, courseId);
        boolean canAddStudent = isAdmin(loginUser) || isCourseTeacher(loginUser, courseId);
        boolean canRemoveStudent = isAdmin(loginUser) || isCourseTeacher(loginUser, courseId);

        return CourseDetailVO.builder()
                .id(course.getId())
                .courseName(course.getCourseName())
                .description(course.getDescription())
                .cover(course.getCover())
                .status(course.getStatus())
                .auditStatus(course.getAuditStatus())
                .auditReason(course.getAuditReason())
                .teachers(teachers)
                .students(students)
                .canAddTeacher(canAddTeacher)
                .canRemoveTeacher(canRemoveTeacher)
                .canAddStudent(canAddStudent)
                .canRemoveStudent(canRemoveStudent)
                .createTime(course.getCreateTime())
                .updateTime(course.getUpdateTime())
                .build();
    }

    @Override
    public PageResult<TeacherSimpleVO> getTeacherCandidates(Long courseId, Integer page, Integer size, String keyword) {
        if (courseId == null) {
            throw new RuntimeException("courseId 不能为空");
        }
        Integer p = page == null || page < 1 ? 1 : page;
        Integer s = size == null || size < 1 ? 10 : size;

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        requireAdminOnly();

        PageHelper.startPage(p, s);

        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getName, keyword));
        }
        qw.apply("id IN (SELECT ur.user_id FROM user_role ur JOIN role r ON ur.role_id = r.id WHERE r.role_code = {0})",
                "TEACHER");
        qw.apply("id NOT IN (SELECT teacher_id FROM course_teacher WHERE course_id = {0})", courseId);
        qw.orderByAsc(User::getId);

        List<User> users = userMapper.selectList(qw);
        PageInfo<User> pageInfo = new PageInfo<>(users);

        if (users == null || users.isEmpty()) {
            return PageResult.of(pageInfo.getTotal(), Collections.emptyList());
        }

        List<TeacherSimpleVO> list = users.stream().map(u -> TeacherSimpleVO.builder()
                .id(u.getId())
                .name(StringUtils.hasText(u.getName()) ? u.getName() : u.getUsername())
                .build()).collect(Collectors.toList());

        return PageResult.of(pageInfo.getTotal(), list);
    }

    @Override
    public PageResult<StudentSimpleVO> getStudentCandidates(Long courseId, Integer page, Integer size, String keyword) {
        if (courseId == null) {
            throw new RuntimeException("courseId 不能为空");
        }
        Integer p = page == null || page < 1 ? 1 : page;
        Integer s = size == null || size < 1 ? 10 : size;

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        requireCourseOwnerOrAdmin(courseId);

        PageHelper.startPage(p, s);

        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getStatus, 1);
        qw.eq(User::getAuditStatus, 1);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getName, keyword)
                    .or().like(User::getPhone, keyword));
        }
        qw.apply("id IN (SELECT ur.user_id FROM user_role ur JOIN role r ON ur.role_id = r.id WHERE r.role_code = {0})",
                "STUDENT");
        qw.apply("id NOT IN (SELECT student_id FROM course_student WHERE course_id = {0})", courseId);
        qw.orderByAsc(User::getId);

        List<User> users = userMapper.selectList(qw);
        PageInfo<User> pageInfo = new PageInfo<>(users);

        if (users == null || users.isEmpty()) {
            return PageResult.of(pageInfo.getTotal(), Collections.emptyList());
        }

        List<StudentSimpleVO> list = users.stream().map(u -> StudentSimpleVO.builder()
                .id(u.getId())
                .name(StringUtils.hasText(u.getName()) ? u.getName() : u.getUsername())
                .build()).collect(Collectors.toList());

        return PageResult.of(pageInfo.getTotal(), list);
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
    public void addTeachers(Long courseId, CourseTeacherAddDTO addDTO) {
        if (courseId == null) {
            throw new RuntimeException("courseId 不能为空");
        }
        if (addDTO == null || addDTO.getTeacherIds() == null || addDTO.getTeacherIds().isEmpty()) {
            throw new RuntimeException("teacherIds 不能为空");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        requireAdminOnly();

        for (Long teacherId : addDTO.getTeacherIds()) {
            if (teacherId == null) {
                continue;
            }

            User teacher = userMapper.selectById(teacherId);
            if (teacher == null) {
                throw new RuntimeException("教师不存在: " + teacherId);
            }
            List<String> roleCodes = userMapper.findRoleCodesByUserId(teacherId);
            if (roleCodes == null || !roleCodes.contains("TEACHER")) {
                throw new RuntimeException("用户不是教师: " + teacherId);
            }

            Long count = courseTeacherMapper.selectCount(new LambdaQueryWrapper<CourseTeacher>()
                    .eq(CourseTeacher::getCourseId, courseId)
                    .eq(CourseTeacher::getTeacherId, teacherId));
            if (count != null && count > 0) {
                continue;
            }

            try {
                courseTeacherMapper.insert(CourseTeacher.builder()
                        .courseId(courseId)
                        .teacherId(teacherId)
                        .build());
            } catch (DuplicateKeyException e) {
                continue;
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTeacher(Long courseId, Long teacherId) {
        if (courseId == null) {
            throw new RuntimeException("courseId 不能为空");
        }
        if (teacherId == null) {
            throw new RuntimeException("teacherId 不能为空");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        requireAdminOrCourseCreator(courseId);

        LoginUser loginUser = requireLoginUser();
        if (!isAdmin(loginUser) && loginUser.getUser() != null && teacherId.equals(loginUser.getUser().getId())) {
            throw new RuntimeException("不能移除自己");
        }

        Long teacherCount = courseTeacherMapper.selectCount(new LambdaQueryWrapper<CourseTeacher>()
                .eq(CourseTeacher::getCourseId, courseId));
        if (teacherCount != null && teacherCount <= 1) {
            throw new RuntimeException("课程至少需要1名教师");
        }

        int rows = courseTeacherMapper.delete(new LambdaQueryWrapper<CourseTeacher>()
                .eq(CourseTeacher::getCourseId, courseId)
                .eq(CourseTeacher::getTeacherId, teacherId));
        if (rows <= 0) {
            throw new RuntimeException("该教师未分配到此课程");
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

        requireCourseOwnerOrAdmin(courseId);

        for (Long studentId : addDTO.getStudentIds()) {
            if (studentId == null) {
                continue;
            }

            User student = userMapper.selectById(studentId);
            if (student == null) {
                throw new RuntimeException("学生不存在: " + studentId);
            }
            List<String> roleCodes = userMapper.findRoleCodesByUserId(studentId);
            if (roleCodes == null || !roleCodes.contains("STUDENT")) {
                throw new RuntimeException("用户不是学生: " + studentId);
            }
            if (student.getAuditStatus() == null || !Objects.equals(student.getAuditStatus(), 1)) {
                throw new RuntimeException("学生未审核通过: " + studentId);
            }

            Long count = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, courseId)
                    .eq(CourseStudent::getStudentId, studentId));
            if (count != null && count > 0) {
                continue;
            }

            try {
                courseStudentMapper.insert(CourseStudent.builder()
                        .courseId(courseId)
                        .studentId(studentId)
                        .build());
            } catch (DuplicateKeyException e) {
                continue;
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeStudent(Long courseId, Long studentId) {
        if (courseId == null) {
            throw new RuntimeException("courseId 不能为空");
        }
        if (studentId == null) {
            throw new RuntimeException("studentId 不能为空");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        requireCourseOwnerOrAdmin(courseId);

        int rows = courseStudentMapper.delete(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStudentId, studentId));
        if (rows <= 0) {
            throw new RuntimeException("该学生未加入此课程");
        }
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

    private boolean isAdmin(LoginUser loginUser) {
        List<String> roleCodes = loginUser == null ? null : loginUser.getRoleCodes();
        return roleCodes != null && roleCodes.contains("ADMIN");
    }

    private void requireAdminOnly() {
        LoginUser loginUser = requireLoginUser();
        if (!isAdmin(loginUser)) {
            throw new RuntimeException("仅管理员可操作");
        }
    }

    private boolean isCourseCreator(LoginUser loginUser, Long courseId) {
        if (loginUser == null || loginUser.getUser() == null || loginUser.getUser().getId() == null) {
            return false;
        }
        List<String> roleCodes = loginUser.getRoleCodes();
        if (roleCodes == null || !roleCodes.contains("TEACHER")) {
            return false;
        }
        Long creatorTeacherId = courseTeacherMapper.findCreatorTeacherIdByCourseId(courseId);
        return creatorTeacherId != null && creatorTeacherId.equals(loginUser.getUser().getId());
    }

    private boolean isCourseTeacher(LoginUser loginUser, Long courseId) {
        if (loginUser == null || loginUser.getUser() == null || loginUser.getUser().getId() == null) {
            return false;
        }
        List<String> roleCodes = loginUser.getRoleCodes();
        if (roleCodes == null || !roleCodes.contains("TEACHER")) {
            return false;
        }
        Long teacherId = loginUser.getUser().getId();
        Long count = courseTeacherMapper.selectCount(new LambdaQueryWrapper<CourseTeacher>()
                .eq(CourseTeacher::getCourseId, courseId)
                .eq(CourseTeacher::getTeacherId, teacherId));
        return count != null && count > 0;
    }

    private void requireAdminOrCourseCreator(Long courseId) {
        LoginUser loginUser = requireLoginUser();
        if (isAdmin(loginUser)) {
            return;
        }
        if (isCourseCreator(loginUser, courseId)) {
            return;
        }
        throw new RuntimeException("仅管理员或课程创建者可操作");
    }

    private void requireCourseMemberOrAdmin(Long courseId) {
        LoginUser loginUser = requireLoginUser();
        List<String> roleCodes = loginUser.getRoleCodes();
        if (roleCodes != null && roleCodes.contains("ADMIN")) {
            return;
        }

        Long userId = loginUser.getUser().getId();
        if (roleCodes != null && roleCodes.contains("TEACHER")) {
            Long count = courseTeacherMapper.selectCount(new LambdaQueryWrapper<CourseTeacher>()
                    .eq(CourseTeacher::getCourseId, courseId)
                    .eq(CourseTeacher::getTeacherId, userId));
            if (count != null && count > 0) {
                return;
            }
        }

        if (roleCodes != null && roleCodes.contains("STUDENT")) {
            Long count = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, courseId)
                    .eq(CourseStudent::getStudentId, userId));
            if (count != null && count > 0) {
                return;
            }
        }

        throw new RuntimeException("没有权限查看该课程");
    }
}
