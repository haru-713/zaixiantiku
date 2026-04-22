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
import com.example.zaixiantiku.exception.BusinessException;
import com.example.zaixiantiku.security.LoginUser;
import com.example.zaixiantiku.utils.SecurityUtils;
import com.example.zaixiantiku.service.CourseService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Course createCourse(CourseCreateDTO createDTO) {
        if (createDTO == null) {
            throw new BusinessException("请求体不能为空");
        }
        if (!StringUtils.hasText(createDTO.getCourseName())) {
            throw new BusinessException("courseName 不能为空");
        }

        String courseName = createDTO.getCourseName().strip();
        Long nameCount = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                .apply("TRIM(course_name) = {0}", courseName));
        if (nameCount != null && nameCount > 0) {
            throw new BusinessException("课程名称已存在");
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
                throw new BusinessException("创建课程失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException("课程名称已存在");
        }

        if (SecurityUtils.hasRole("TEACHER")) {
            CourseTeacher courseTeacher = CourseTeacher.builder()
                    .courseId(course.getId())
                    .teacherId(SecurityUtils.getUserId())
                    .build();
            courseTeacherMapper.insert(courseTeacher);
        }

        return course;
    }

    @Override
    public PageResult<CourseListVO> getCoursePage(CourseQueryDTO queryDTO) {
        LoginUser loginUser = SecurityUtils.requireLoginUser();
        Long userId = SecurityUtils.getUserId();
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
            throw new BusinessException("courseId 不能为空");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }

        SecurityUtils.requireLoginUser();
        requireCourseMemberOrAdmin(courseId);

        List<CourseTeacherRowVO> teacherRows = courseTeacherMapper.findTeachersByCourseIds(List.of(courseId));
        List<TeacherSimpleVO> teachers = teacherRows == null ? Collections.emptyList()
                : teacherRows.stream()
                        .map(r -> TeacherSimpleVO.builder().id(r.getId()).name(r.getName()).build())
                        .collect(Collectors.toList());

        List<StudentSimpleVO> students = Collections.emptyList();
        if (SecurityUtils.isAdmin(SecurityUtils.getLoginUser()) || isCourseTeacher(courseId)) {
            students = courseStudentMapper.findStudentsByCourseId(courseId);
        }

        boolean canAddTeacher = SecurityUtils.isAdmin(SecurityUtils.getLoginUser());
        boolean canRemoveTeacher = SecurityUtils.isAdmin(SecurityUtils.getLoginUser()) || isCourseCreator(courseId);
        boolean canAddStudent = SecurityUtils.isAdmin(SecurityUtils.getLoginUser()) || isCourseTeacher(courseId);
        boolean canRemoveStudent = SecurityUtils.isAdmin(SecurityUtils.getLoginUser()) || isCourseTeacher(courseId);

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
            throw new BusinessException("courseId 不能为空");
        }
        Integer p = page == null || page < 1 ? 1 : page;
        Integer s = size == null || size < 1 ? 10 : size;

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
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
            throw new BusinessException("courseId 不能为空");
        }
        Integer p = page == null || page < 1 ? 1 : page;
        Integer s = size == null || size < 1 ? 10 : size;

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
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
        LoginUser loginUser = SecurityUtils.requireLoginUser();
        Long userId = SecurityUtils.getUserId();
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
            throw new BusinessException("courseId 不能为空");
        }
        if (auditDTO == null || auditDTO.getAuditStatus() == null) {
            throw new BusinessException("auditStatus 不能为空");
        }
        if (!Objects.equals(auditDTO.getAuditStatus(), 1) && !Objects.equals(auditDTO.getAuditStatus(), 2)) {
            throw new BusinessException("auditStatus 参数非法");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }

        Course update = new Course();
        update.setId(courseId);
        update.setAuditStatus(auditDTO.getAuditStatus());
        update.setAuditReason(auditDTO.getReason());
        int rows = courseMapper.updateById(update);
        if (rows != 1) {
            throw new BusinessException("审核失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCourseStatus(Long courseId, CourseStatusDTO statusDTO) {
        if (courseId == null) {
            throw new BusinessException("courseId 不能为空");
        }
        if (statusDTO == null || statusDTO.getStatus() == null) {
            throw new BusinessException("status 不能为空");
        }
        if (!Objects.equals(statusDTO.getStatus(), 0) && !Objects.equals(statusDTO.getStatus(), 1)) {
            throw new BusinessException("status 参数非法");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }

        Course update = new Course();
        update.setId(courseId);
        update.setStatus(statusDTO.getStatus());
        int rows = courseMapper.updateById(update);
        if (rows != 1) {
            throw new BusinessException("状态更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Course updateCourse(Long courseId, CourseUpdateDTO updateDTO) {
        if (courseId == null) {
            throw new BusinessException("courseId 不能为空");
        }
        if (updateDTO == null) {
            throw new BusinessException("请求体不能为空");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
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
                throw new BusinessException("课程名称已存在");
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
            throw new BusinessException("更新课程失败");
        }
        return courseMapper.selectById(courseId);
    }

    @Override
    public PageResult<StudentSimpleVO> getCourseStudents(Long courseId, Long classId, Integer page, Integer size,
            String keyword) {
        if (courseId == null) {
            throw new BusinessException("courseId 不能为空");
        }
        Integer p = page == null || page < 1 ? 1 : page;
        Integer s = size == null || size < 1 ? 10 : size;

        requireCourseMemberOrAdmin(courseId);

        PageHelper.startPage(p, s);
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getName, keyword));
        }
        qw.apply("id IN (SELECT student_id FROM course_student WHERE course_id = {0})", courseId);
        if (classId != null) {
            qw.apply("id IN (SELECT student_id FROM student_class WHERE class_id = {0})", classId);
        }
        qw.orderByAsc(User::getId);

        List<User> users = userMapper.selectList(qw);
        PageInfo<User> pageInfo = new PageInfo<>(users);

        if (users == null || users.isEmpty()) {
            return PageResult.of(pageInfo.getTotal(), Collections.emptyList());
        }

        List<StudentSimpleVO> list = users.stream().map(u -> {
            StudentSimpleVO vo = StudentSimpleVO.builder()
                    .id(u.getId())
                    .name(StringUtils.hasText(u.getName()) ? u.getName() : u.getUsername())
                    .username(u.getUsername())
                    .avatar(u.getAvatar())
                    .build();
            // 获取班级名称
            try {
                String className = jdbcTemplate.queryForObject(
                        "SELECT class_name FROM class c JOIN student_class sc ON c.id = sc.class_id WHERE sc.student_id = ? LIMIT 1",
                        String.class, u.getId());
                vo.setClassName(className);
            } catch (Exception e) {
                vo.setClassName("未分配班级");
            }
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(pageInfo.getTotal(), list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddClassStudentsToCourse(Long courseId, Long classId) {
        if (courseId == null || classId == null) {
            throw new BusinessException("courseId 和 classId 不能为空");
        }
        requireCourseOwnerOrAdmin(courseId);

        // 获取班级下的所有学生
        List<Long> studentIds = jdbcTemplate.queryForList(
                "SELECT student_id FROM student_class WHERE class_id = ?",
                Long.class, classId);

        if (studentIds.isEmpty()) {
            return;
        }

        for (Long studentId : studentIds) {
            Long count = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, courseId)
                    .eq(CourseStudent::getStudentId, studentId));
            if (count == 0) {
                CourseStudent cs = CourseStudent.builder()
                        .courseId(courseId)
                        .studentId(studentId)
                        .joinTime(LocalDateTime.now())
                        .build();
                courseStudentMapper.insert(cs);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourse(Long courseId) {
        if (courseId == null) {
            throw new BusinessException("courseId 不能为空");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }

        // 核心资源校验：无论管理员还是普通教师，若存在核心教学资源，均禁止删除
        // 1. 检查学生关联
        Long studentCount = courseStudentMapper
                .selectCount(new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getCourseId, courseId));
        if (studentCount != null && studentCount > 0) {
            throw new BusinessException("该课程下已有选课学生，禁止删除课程。请先清退学生或由管理员处理。");
        }

        // 2. 检查知识点
        Integer kpCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM knowledge_point WHERE course_id = ?",
                Integer.class, courseId);
        if (kpCount != null && kpCount > 0) {
            throw new BusinessException("该课程下有关联的知识点，请先删除知识点后再删除课程");
        }

        // 3. 检查题目
        Integer questionCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM question WHERE course_id = ?",
                Integer.class, courseId);
        if (questionCount != null && questionCount > 0) {
            throw new BusinessException("该课程下有关联的题目，请先清空题目后再删除课程");
        }

        // 4. 检查试卷 (包含 paper_question 级联检查)
        Integer paperCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM paper WHERE course_id = ?",
                Integer.class, courseId);
        if (paperCount != null && paperCount > 0) {
            throw new BusinessException("该课程下有关联的试卷，请先删除试卷后再删除课程");
        }

        // 权限检查
        if (!SecurityUtils.isAdmin(SecurityUtils.getLoginUser())) {
            requireCourseOwnerOrAdmin(courseId);
        }

        // 清理基础关联数据
        // 1. 练习记录
        jdbcTemplate.update("DELETE FROM practice_record WHERE course_id = ?", courseId);
        // 2. 师生关联 (不再作为阻碍，直接清理)
        jdbcTemplate.update("DELETE FROM course_teacher WHERE course_id = ?", courseId);

        // 5. 删除课程
        int rows = courseMapper.deleteById(courseId);
        if (rows != 1) {
            throw new RuntimeException("删除课程失败");
        }
    }

    private boolean existsTable(String tableName) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName);
        return c != null && c > 0;
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
            throw new BusinessException("courseId 不能为空");
        }
        if (teacherId == null) {
            throw new BusinessException("teacherId 不能为空");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }

        SecurityUtils.requireLoginUser();

        // 校验：课程至少保留一名教师
        Long teacherCount = courseTeacherMapper.selectCount(new LambdaQueryWrapper<CourseTeacher>()
                .eq(CourseTeacher::getCourseId, courseId));
        if (teacherCount != null && teacherCount <= 1) {
            throw new BusinessException("课程至少需要一名任课教师，无法移除最后一位。若需删除课程，请直接点击“删除课程”按钮。");
        }

        // 权限校验：仅管理员或课程创建者可移除教师，且教师可退出自己教授的课程
        boolean isAdmin = SecurityUtils.isAdmin(SecurityUtils.getLoginUser());
        boolean isCreator = isCourseCreator(courseId);
        boolean isSelf = Objects.equals(teacherId, SecurityUtils.getUserId());

        if (!isAdmin && !isCreator && !isSelf) {
            throw new BusinessException("无权移除该教师");
        }

        courseTeacherMapper.delete(new LambdaQueryWrapper<CourseTeacher>()
                .eq(CourseTeacher::getCourseId, courseId)
                .eq(CourseTeacher::getTeacherId, teacherId));
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
            throw new BusinessException("courseId 不能为空");
        }
        if (addDTO == null || addDTO.getStudentIds() == null || addDTO.getStudentIds().isEmpty()) {
            throw new BusinessException("studentIds 不能为空");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }

        requireCourseOwnerOrAdmin(courseId);

        for (Long studentId : addDTO.getStudentIds()) {
            if (studentId == null) {
                continue;
            }

            User student = userMapper.selectById(studentId);
            if (student == null) {
                throw new BusinessException("学生不存在: " + studentId);
            }
            List<String> roleCodes = userMapper.findRoleCodesByUserId(studentId);
            if (roleCodes == null || !roleCodes.contains("STUDENT")) {
                throw new BusinessException("用户不是学生: " + studentId);
            }
            if (student.getAuditStatus() == null || !Objects.equals(student.getAuditStatus(), 1)) {
                throw new BusinessException("学生未审核通过: " + studentId);
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
            throw new BusinessException("courseId 不能为空");
        }
        if (studentId == null) {
            throw new BusinessException("studentId 不能为空");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }

        requireCourseOwnerOrAdmin(courseId);

        int rows = courseStudentMapper.delete(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStudentId, studentId));
        if (rows <= 0) {
            throw new BusinessException("该学生未加入此课程");
        }
    }

    @Override
    public List<Course> getManagedCourses() {
        LoginUser loginUser = SecurityUtils.requireLoginUser();
        List<String> roles = loginUser.getRoleCodes();
        boolean isAdmin = roles != null && roles.contains("ADMIN");

        if (isAdmin) {
            return courseMapper.selectList(new LambdaQueryWrapper<Course>().eq(Course::getStatus, 1));
        }

        Long teacherId = SecurityUtils.getUserId();
        List<Long> managedCourseIds = courseTeacherMapper.selectList(new LambdaQueryWrapper<CourseTeacher>()
                .eq(CourseTeacher::getTeacherId, teacherId))
                .stream().map(CourseTeacher::getCourseId).collect(Collectors.toList());

        if (managedCourseIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        return courseMapper.selectBatchIds(managedCourseIds).stream()
                .filter(c -> c.getStatus() == 1)
                .collect(Collectors.toList());
    }

    private void requireCourseOwnerOrAdmin(Long courseId) {
        if (SecurityUtils.isAdmin(SecurityUtils.getLoginUser())) {
            return;
        }
        if (!SecurityUtils.hasRole("TEACHER")) {
            throw new BusinessException("没有权限操作该课程");
        }
        Long teacherId = SecurityUtils.getUserId();
        Long count = courseTeacherMapper.selectCount(new LambdaQueryWrapper<CourseTeacher>()
                .eq(CourseTeacher::getCourseId, courseId)
                .eq(CourseTeacher::getTeacherId, teacherId));
        if (count == null || count <= 0) {
            throw new BusinessException("没有权限操作该课程");
        }
    }

    private void requireAdminOnly() {
        SecurityUtils.requireAdmin();
    }

    private boolean isCourseCreator(Long courseId) {
        if (!SecurityUtils.hasRole("TEACHER")) {
            return false;
        }
        Long creatorTeacherId = courseTeacherMapper.findCreatorTeacherIdByCourseId(courseId);
        return creatorTeacherId != null && creatorTeacherId.equals(SecurityUtils.getUserId());
    }

    private boolean isCourseTeacher(Long courseId) {
        if (!SecurityUtils.hasRole("TEACHER")) {
            return false;
        }
        Long teacherId = SecurityUtils.getUserId();
        Long count = courseTeacherMapper.selectCount(new LambdaQueryWrapper<CourseTeacher>()
                .eq(CourseTeacher::getCourseId, courseId)
                .eq(CourseTeacher::getTeacherId, teacherId));
        return count != null && count > 0;
    }

    private void requireCourseMemberOrAdmin(Long courseId) {
        if (SecurityUtils.isAdmin(SecurityUtils.getLoginUser())) {
            return;
        }

        Long userId = SecurityUtils.getUserId();
        if (SecurityUtils.hasRole("TEACHER")) {
            Long count = courseTeacherMapper.selectCount(new LambdaQueryWrapper<CourseTeacher>()
                    .eq(CourseTeacher::getCourseId, courseId)
                    .eq(CourseTeacher::getTeacherId, userId));
            if (count != null && count > 0) {
                return;
            }
        }
        if (SecurityUtils.hasRole("STUDENT")) {
            Long count = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, courseId)
                    .eq(CourseStudent::getStudentId, userId));
            if (count != null && count > 0) {
                return;
            }
        }
        throw new BusinessException("您未参加该课程，无权访问");
    }
}
