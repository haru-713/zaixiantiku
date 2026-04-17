package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.zaixiantiku.entity.CourseTeacher;
import com.example.zaixiantiku.entity.Exam;
import com.example.zaixiantiku.entity.ExamClass;
import com.example.zaixiantiku.entity.ExamStudent;
import com.example.zaixiantiku.mapper.CourseTeacherMapper;
import com.example.zaixiantiku.mapper.ExamClassMapper;
import com.example.zaixiantiku.mapper.ExamMapper;
import com.example.zaixiantiku.mapper.ExamStudentMapper;
import com.example.zaixiantiku.pojo.dto.ExamSaveDTO;
import com.example.zaixiantiku.pojo.vo.ExamVO;
import com.example.zaixiantiku.security.LoginUser;
import com.example.zaixiantiku.service.ExamService;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 考试服务实现类
 */
@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamMapper examMapper;
    private final ExamClassMapper examClassMapper;
    private final ExamStudentMapper examStudentMapper;
    private final CourseTeacherMapper courseTeacherMapper;
    private final com.example.zaixiantiku.mapper.PaperMapper paperMapper;

    @Override
    public PageResult<ExamVO> getExamPage(Integer page, Integer size, String keyword, Long courseId, Integer status) {
        PageHelper.startPage(page, size);
        LambdaQueryWrapper<Exam> qw = new LambdaQueryWrapper<>();
        if (courseId != null) {
            qw.eq(Exam::getCourseId, courseId);
        }
        if (status != null) {
            qw.eq(Exam::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            qw.like(Exam::getExamName, keyword);
        }
        qw.orderByDesc(Exam::getId);
        List<Exam> list = examMapper.selectList(qw);
        PageInfo<Exam> pageInfo = new PageInfo<>(list);

        // 获取试卷名称映射
        List<Long> paperIds = list.stream().map(Exam::getPaperId).distinct().collect(Collectors.toList());
        java.util.Map<Long, String> paperNameMap = new java.util.HashMap<>();
        if (!paperIds.isEmpty()) {
            List<com.example.zaixiantiku.entity.Paper> papers = paperMapper.selectBatchIds(paperIds);
            paperNameMap = papers.stream().collect(Collectors.toMap(com.example.zaixiantiku.entity.Paper::getId,
                    com.example.zaixiantiku.entity.Paper::getPaperName));
        }

        java.util.Map<Long, String> finalPaperNameMap = paperNameMap;
        List<ExamVO> voList = list.stream().map(exam -> {
            ExamVO vo = this.toVO(exam);
            vo.setPaperName(finalPaperNameMap.get(exam.getPaperId()));
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(pageInfo.getTotal(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamVO createExam(ExamSaveDTO saveDTO) {
        if (saveDTO.getStartTime() != null && saveDTO.getEndTime() != null) {
            if (saveDTO.getEndTime().isBefore(saveDTO.getStartTime())) {
                throw new RuntimeException("考试结束时间不能早于开始时间");
            }
        }
        LoginUser loginUser = requireLoginUser();
        Long creatorId = loginUser.getUser().getId();
        requireTeacherOfCourseOrAdmin(loginUser, saveDTO.getCourseId());

        Exam exam = Exam.builder()
                .examName(saveDTO.getExamName())
                .paperId(saveDTO.getPaperId())
                .courseId(saveDTO.getCourseId())
                .startTime(saveDTO.getStartTime())
                .endTime(saveDTO.getEndTime())
                .duration(saveDTO.getDuration())
                .status(0) // 默认未开始
                .publishScore(saveDTO.getPublishScore() != null ? saveDTO.getPublishScore() : 0)
                .createBy(creatorId)
                .build();

        examMapper.insert(exam);

        // 保存班级关联
        saveExamClasses(exam.getId(), saveDTO.getClassIds());
        // 保存学生关联
        saveExamStudents(exam.getId(), saveDTO.getStudentIds());

        return toVO(exam);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamVO updateExam(Long examId, ExamSaveDTO saveDTO) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }

        LoginUser loginUser = requireLoginUser();
        requireTeacherOfCourseOrAdmin(loginUser, exam.getCourseId());

        if (saveDTO.getStartTime() != null && saveDTO.getEndTime() != null) {
            if (saveDTO.getEndTime().isBefore(saveDTO.getStartTime())) {
                throw new RuntimeException("考试结束时间不能早于开始时间");
            }
        }

        // 更新基本信息
        exam.setExamName(saveDTO.getExamName());
        exam.setPaperId(saveDTO.getPaperId());
        exam.setStartTime(saveDTO.getStartTime());
        exam.setEndTime(saveDTO.getEndTime());
        exam.setDuration(saveDTO.getDuration());
        if (saveDTO.getPublishScore() != null) {
            exam.setPublishScore(saveDTO.getPublishScore());
        }

        examMapper.updateById(exam);

        // 更新关联：先删后增
        examClassMapper.delete(new LambdaQueryWrapper<ExamClass>().eq(ExamClass::getExamId, examId));
        saveExamClasses(examId, saveDTO.getClassIds());

        examStudentMapper.delete(new LambdaQueryWrapper<ExamStudent>().eq(ExamStudent::getExamId, examId));
        saveExamStudents(examId, saveDTO.getStudentIds());

        return toVO(exam);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }

        LoginUser loginUser = requireLoginUser();
        requireTeacherOfCourseOrAdmin(loginUser, exam.getCourseId());

        // 如果已有考试记录（状态为进行中或已结束），不允许删除
        if (exam.getStatus() != 0) {
            throw new RuntimeException("只能取消未开始的考试");
        }

        // 删除关联
        examClassMapper.delete(new LambdaQueryWrapper<ExamClass>().eq(ExamClass::getExamId, examId));
        examStudentMapper.delete(new LambdaQueryWrapper<ExamStudent>().eq(ExamStudent::getExamId, examId));

        // 删除考试
        examMapper.deleteById(examId);
    }

    private void saveExamClasses(Long examId, List<Long> classIds) {
        if (classIds != null && !classIds.isEmpty()) {
            for (Long classId : classIds) {
                ExamClass ec = ExamClass.builder()
                        .examId(examId)
                        .classId(classId)
                        .build();
                examClassMapper.insert(ec);
            }
        }
    }

    private void saveExamStudents(Long examId, List<Long> studentIds) {
        if (studentIds != null && !studentIds.isEmpty()) {
            for (Long studentId : studentIds) {
                ExamStudent es = ExamStudent.builder()
                        .examId(examId)
                        .studentId(studentId)
                        .build();
                examStudentMapper.insert(es);
            }
        }
    }

    private ExamVO toVO(Exam exam) {
        Integer status = exam.getStatus();
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime())) {
            status = 0;
        } else if (now.isAfter(exam.getEndTime())) {
            status = 2;
        } else {
            status = 1;
        }

        return ExamVO.builder()
                .id(exam.getId())
                .examName(exam.getExamName())
                .paperId(exam.getPaperId())
                .courseId(exam.getCourseId())
                .startTime(exam.getStartTime())
                .endTime(exam.getEndTime())
                .duration(exam.getDuration())
                .status(status)
                .publishScore(exam.getPublishScore())
                .createBy(exam.getCreateBy())
                .createTime(exam.getCreateTime())
                .build();
    }

    private LoginUser requireLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("未登录");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw new RuntimeException("未登录");
    }

    private void requireTeacherOfCourseOrAdmin(LoginUser loginUser, Long courseId) {
        List<String> roles = loginUser.getRoleCodes();
        if (roles != null && roles.contains("ADMIN")) {
            return;
        }
        if (roles == null || !roles.contains("TEACHER")) {
            throw new RuntimeException("没有权限操作");
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
