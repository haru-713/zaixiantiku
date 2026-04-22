package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.zaixiantiku.entity.*;
import com.example.zaixiantiku.mapper.*;
import com.example.zaixiantiku.pojo.dto.ExamMarkDTO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.TeacherExamRecordVO;
import com.example.zaixiantiku.security.LoginUser;
import com.example.zaixiantiku.service.TeacherExamService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherExamServiceImpl implements TeacherExamService {

    private final ExamRecordMapper examRecordMapper;
    private final ExamMapper examMapper;
    private final PaperMapper paperMapper;
    private final UserMapper userMapper;
    private final AnswerDetailMapper answerDetailMapper;
    private final CourseTeacherMapper courseTeacherMapper;
    private final LogMapper logMapper;

    @Override
    public PageResult<TeacherExamRecordVO> getPendingMarkingRecords(Long courseId, Integer status, Integer page,
            Integer size) {
        LoginUser loginUser = requireLoginUser();
        Long userId = loginUser.getUser().getId();
        boolean isAdmin = loginUser.getRoleCodes().contains("ADMIN");

        PageHelper.startPage(page, size);

        // 1. 基础过滤：状态过滤
        LambdaQueryWrapper<ExamRecord> qw = new LambdaQueryWrapper<ExamRecord>()
                .ne(ExamRecord::getStatus, 0) // 始终过滤掉“考试中”的记录
                .orderByDesc(ExamRecord::getSubmitTime);

        if (status != null) {
            qw.eq(ExamRecord::getStatus, status);
        } else {
            // 默认显示待批阅
            qw.eq(ExamRecord::getStatus, 1);
        }

        // 2. 权限过滤：如果是老师，只能看自己教授课程的考试
        Set<Long> accessibleExamIds = new HashSet<>();
        if (!isAdmin) {
            // 获取老师教授的所有课程
            List<Long> teacherCourseIds = courseTeacherMapper.selectList(new LambdaQueryWrapper<CourseTeacher>()
                    .eq(CourseTeacher::getTeacherId, userId))
                    .stream().map(CourseTeacher::getCourseId).collect(Collectors.toList());

            if (teacherCourseIds.isEmpty()) {
                return PageResult.of(0L, new ArrayList<>());
            }

            // 获取这些课程关联的所有考试ID
            accessibleExamIds = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                    .in(Exam::getCourseId, teacherCourseIds))
                    .stream().map(Exam::getId).collect(Collectors.toSet());

            if (accessibleExamIds.isEmpty()) {
                return PageResult.of(0L, new ArrayList<>());
            }

            qw.in(ExamRecord::getExamId, accessibleExamIds);
        }

        // 3. 课程过滤 (前端传入参数)
        if (courseId != null) {
            List<Long> examIdsByCourse = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                    .eq(Exam::getCourseId, courseId))
                    .stream().map(Exam::getId).collect(Collectors.toList());
            if (examIdsByCourse.isEmpty()) {
                return PageResult.of(0L, new ArrayList<>());
            }
            qw.in(ExamRecord::getExamId, examIdsByCourse);
        }

        List<ExamRecord> records = examRecordMapper.selectList(qw);
        PageInfo<ExamRecord> pageInfo = new PageInfo<>(records);

        if (records.isEmpty()) {
            return PageResult.of(0L, new ArrayList<>());
        }

        // 4. 组装数据
        List<Long> recordExamIds = records.stream().map(ExamRecord::getExamId).distinct().collect(Collectors.toList());
        List<Long> userIds = records.stream().map(ExamRecord::getUserId).distinct().collect(Collectors.toList());

        Map<Long, Exam> examMap = examMapper.selectBatchIds(recordExamIds).stream()
                .collect(Collectors.toMap(Exam::getId, e -> e));

        List<Long> paperIds = examMap.values().stream().map(Exam::getPaperId).distinct().collect(Collectors.toList());
        Map<Long, String> paperNameMapTemp = new HashMap<>();
        if (!paperIds.isEmpty()) {
            paperNameMapTemp = paperMapper.selectBatchIds(paperIds).stream()
                    .collect(Collectors.toMap(Paper::getId, Paper::getPaperName));
        }
        final Map<Long, String> paperNameMap = paperNameMapTemp;

        Map<Long, String> userNameMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getName() != null ? u.getName() : u.getUsername()));

        // 获取课程教师和每场考试的批阅统计
        Map<Long, String> examTeachersMap = new HashMap<>();
        Map<Long, Integer> examTotalCountMap = new HashMap<>();
        Map<Long, Integer> examMarkedCountMap = new HashMap<>();

        for (Long examId : recordExamIds) {
            Exam exam = examMap.get(examId);
            if (exam != null) {
                // 获取任课教师
                List<CourseTeacher> cts = courseTeacherMapper.selectList(
                        new LambdaQueryWrapper<CourseTeacher>().eq(CourseTeacher::getCourseId, exam.getCourseId()));
                if (!cts.isEmpty()) {
                    List<Long> tIds = cts.stream().map(CourseTeacher::getTeacherId).collect(Collectors.toList());
                    String names = userMapper.selectBatchIds(tIds).stream()
                            .map(u -> u.getName() != null ? u.getName() : u.getUsername())
                            .collect(Collectors.joining(","));
                    examTeachersMap.put(examId, names);
                }

                // 统计该场考试的批阅情况（所有交卷的学生：状态 1 或 2）
                Long totalSubmitted = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getExamId, examId)
                        .in(ExamRecord::getStatus, Arrays.asList(1, 2)));
                Long marked = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getExamId, examId)
                        .eq(ExamRecord::getStatus, 2));
                examTotalCountMap.put(examId, totalSubmitted.intValue());
                examMarkedCountMap.put(examId, marked.intValue());
            }
        }

        List<TeacherExamRecordVO> voList = records.stream().map(r -> {
            Exam e = examMap.get(r.getExamId());
            TeacherExamRecordVO vo = TeacherExamRecordVO.builder()
                    .id(r.getId())
                    .examId(r.getExamId())
                    .examName(e != null ? e.getExamName() : "未知考试")
                    .paperName(e != null ? paperNameMap.get(e.getPaperId()) : "未知试卷")
                    .studentName(userNameMap.get(r.getUserId()))
                    .submitTime(r.getSubmitTime())
                    .totalScore(r.getTotalScore())
                    .status(r.getStatus())
                    .courseTeacherNames(examTeachersMap.get(r.getExamId()))
                    .totalExamCount(examTotalCountMap.get(r.getExamId()))
                    .markedExamCount(examMarkedCountMap.get(r.getExamId()))
                    .build();

            // 从日志表查询作弊信息（因为 exam_record 表中没有 answers 字段）
            try {
                String examIdStr = "\"examId\":" + r.getExamId();
                // 1. 统计切屏次数
                Long cheatCount = logMapper.selectCount(new LambdaQueryWrapper<Log>()
                        .eq(Log::getUserId, r.getUserId())
                        .eq(Log::getOperation, "切屏")
                        .like(Log::getParams, examIdStr));
                vo.setCheatCount(cheatCount.intValue());

                // 2. 检查是否有强制交卷记录
                Long forceSubmitCount = logMapper.selectCount(new LambdaQueryWrapper<Log>()
                        .eq(Log::getUserId, r.getUserId())
                        .eq(Log::getOperation, "强制交卷")
                        .like(Log::getParams, examIdStr));

                if (forceSubmitCount > 0 || cheatCount >= 3) {
                    vo.setForceSubmit(true);
                }
            } catch (Exception ex) {
                log.error("查询切屏日志失败", ex);
            }
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(pageInfo.getTotal(), voList);
    }

    @Override
    public List<Long> getPendingMarkingRecordIds(Long examId) {
        // 查询该考试下所有状态为1（待阅卷）的记录，并按用户ID（通常关联学号）排序
        return examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getStatus, 1)
                .orderByAsc(ExamRecord::getUserId))
                .stream()
                .map(ExamRecord::getId)
                .collect(Collectors.toList());
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> markExamRecord(Long recordId, ExamMarkDTO markDTO) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("考试记录不存在");
        }
        if (record.getStatus() != 1) {
            throw new RuntimeException("该考试记录状态不是待批阅状态");
        }

        int additionalScore = 0;
        for (ExamMarkDTO.MarkItem item : markDTO.getScores()) {
            AnswerDetail detail = answerDetailMapper.selectOne(new LambdaQueryWrapper<AnswerDetail>()
                    .eq(AnswerDetail::getExamRecordId, recordId)
                    .eq(AnswerDetail::getQuestionId, item.getQuestionId()));

            if (detail != null) {
                // 更新得分，这里假设 score 之前是 0 或者 null
                int oldScore = detail.getScore() != null ? detail.getScore() : 0;
                detail.setScore(item.getScore());
                // 阅卷通常认为只要阅卷了就可能是对的（部分正确也算1，或者根据分数判断）
                detail.setIsCorrect(item.getScore() > 0 ? 1 : 0);
                answerDetailMapper.updateById(detail);

                additionalScore += (item.getScore() - oldScore);
            }
        }

        // 更新总分和状态
        record.setTotalScore((record.getTotalScore() != null ? record.getTotalScore() : 0) + additionalScore);
        record.setStatus(2); // 已批阅
        examRecordMapper.updateById(record);

        Map<String, Object> result = new HashMap<>();
        result.put("totalScore", record.getTotalScore());
        return result;
    }
}
