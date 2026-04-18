package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.zaixiantiku.entity.*;
import com.example.zaixiantiku.mapper.*;
import com.example.zaixiantiku.pojo.dto.ExamSubmitDTO;
import com.example.zaixiantiku.pojo.vo.*;
import com.example.zaixiantiku.security.LoginUser;
import com.example.zaixiantiku.service.StudentExamService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentExamServiceImpl implements StudentExamService {

    private final ExamMapper examMapper;
    private final ExamClassMapper examClassMapper;
    private final ExamStudentMapper examStudentMapper;
    private final ExamRecordMapper examRecordMapper;
    private final AnswerDetailMapper answerDetailMapper;
    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionTypeMapper questionTypeMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final StudentClassMapper studentClassMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<ExamVO> getStudentExams(Long courseId) {
        LoginUser loginUser = requireLoginUser();
        Long userId = loginUser.getUser().getId();

        // 1. 获取该学生已参加的课程ID (基础过滤：必须参加了课程才能看该课程的考试)
        List<Long> enrolledCourseIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getStudentId, userId))
                .stream().map(CourseStudent::getCourseId).collect(Collectors.toList());

        if (enrolledCourseIds.isEmpty()) {
            return PageResult.of(0L, new ArrayList<>());
        }

        // 2. 获取该学生所属的所有班级ID
        List<Long> classIds = studentClassMapper.selectList(new LambdaQueryWrapper<StudentClass>()
                .eq(StudentClass::getStudentId, userId))
                .stream().map(StudentClass::getClassId).collect(Collectors.toList());

        // 3. 查询符合课程条件的考试
        LambdaQueryWrapper<Exam> qw = new LambdaQueryWrapper<>();
        qw.in(Exam::getCourseId, enrolledCourseIds);
        if (courseId != null) {
            qw.eq(Exam::getCourseId, courseId);
        }
        qw.orderByDesc(Exam::getStartTime);

        List<Exam> exams = examMapper.selectList(qw);
        if (exams.isEmpty()) {
            return PageResult.of(0L, new ArrayList<>());
        }

        // 4. 权限过滤
        List<Long> allExamIds = exams.stream().map(Exam::getId).collect(Collectors.toList());

        // 批量查询有班级关联的考试ID
        Set<Long> hasClassAssignedExamIds = examClassMapper.selectList(new LambdaQueryWrapper<ExamClass>()
                .in(ExamClass::getExamId, allExamIds))
                .stream().map(ExamClass::getExamId).collect(Collectors.toSet());

        // 批量查询有个人关联的考试ID
        Set<Long> hasStudentAssignedExamIds = examStudentMapper.selectList(new LambdaQueryWrapper<ExamStudent>()
                .in(ExamStudent::getExamId, allExamIds))
                .stream().map(ExamStudent::getExamId).collect(Collectors.toSet());

        // 查询当前学生所在班级关联的考试ID
        Set<Long> myClassExamIds = new HashSet<>();
        if (!classIds.isEmpty()) {
            myClassExamIds = examClassMapper.selectList(new LambdaQueryWrapper<ExamClass>()
                    .in(ExamClass::getExamId, allExamIds)
                    .in(ExamClass::getClassId, classIds))
                    .stream().map(ExamClass::getExamId).collect(Collectors.toSet());
        }

        // 查询直接分配给当前学生的考试ID
        Set<Long> myDirectExamIds = examStudentMapper.selectList(new LambdaQueryWrapper<ExamStudent>()
                .in(ExamStudent::getExamId, allExamIds)
                .eq(ExamStudent::getStudentId, userId))
                .stream().map(ExamStudent::getExamId).collect(Collectors.toSet());

        Set<Long> finalMyClassExamIds = myClassExamIds;
        List<Exam> filteredExams = exams.stream().filter(e -> {
            boolean hasClassAssign = hasClassAssignedExamIds.contains(e.getId());
            boolean hasStudentAssign = hasStudentAssignedExamIds.contains(e.getId());

            // 没有任何分配 -> 全员开放
            if (!hasClassAssign && !hasStudentAssign) {
                return true;
            }
            // 分配给了我的班级
            if (finalMyClassExamIds.contains(e.getId())) {
                return true;
            }
            // 直接分配给了我
            if (myDirectExamIds.contains(e.getId())) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());

        // 获取试卷名映射
        List<Long> paperIds = filteredExams.stream().map(Exam::getPaperId).distinct().collect(Collectors.toList());
        Map<Long, String> paperNameMap = new HashMap<>();
        if (!paperIds.isEmpty()) {
            paperNameMap = paperMapper.selectBatchIds(paperIds).stream()
                    .collect(Collectors.toMap(Paper::getId, Paper::getPaperName));
        }

        Map<Long, String> finalPaperNameMap = paperNameMap;
        List<ExamVO> voList = filteredExams.stream().map(e -> {
            ExamVO vo = toExamVO(e);
            vo.setPaperName(finalPaperNameMap.get(e.getPaperId()));
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of((long) voList.size(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamEnterVO enterExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null)
            throw new RuntimeException("考试不存在");

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime()))
            throw new RuntimeException("考试尚未开始");
        if (now.isAfter(exam.getEndTime()))
            throw new RuntimeException("考试已结束");

        LoginUser loginUser = requireLoginUser();
        Long userId = loginUser.getUser().getId();

        // 获取该学生所属的所有班级ID
        List<Long> classIds = studentClassMapper.selectList(new LambdaQueryWrapper<StudentClass>()
                .eq(StudentClass::getStudentId, userId))
                .stream().map(StudentClass::getClassId).collect(Collectors.toList());

        // 校验权限
        checkStudentExamPermission(exam, userId, classIds);

        // 检查是否已有记录
        ExamRecord record = examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getUserId, userId));

        if (record == null) {
            // 创建新记录
            record = ExamRecord.builder()
                    .examId(examId)
                    .userId(userId)
                    .startTime(now)
                    .status(0) // 考试中
                    .build();
            examRecordMapper.insert(record);
        } else if (record.getStatus() != 0) {
            throw new RuntimeException("您已提交过该考试");
        }

        // 获取试卷内容
        Paper paper = paperMapper.selectById(exam.getPaperId());
        List<PaperQuestion> pqList = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paper.getId())
                .orderByAsc(PaperQuestion::getSortOrder));

        List<Long> qIds = pqList.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> qMap = questionMapper.selectBatchIds(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<ExamEnterVO.QuestionInfo> questions = pqList.stream().map(pq -> {
            Question q = qMap.get(pq.getQuestionId());
            return ExamEnterVO.QuestionInfo.builder()
                    .id(q.getId())
                    .content(q.getContent())
                    .typeId(q.getTypeId().intValue())
                    // 这里需要处理 JSON 格式的 options
                    .options(parseOptions(q.getOptions()))
                    .sortOrder(pq.getSortOrder())
                    .score(pq.getScore())
                    .build();
        }).collect(Collectors.toList());

        // 计算剩余时间
        long elapsedSeconds = Duration.between(record.getStartTime(), now).getSeconds();
        long totalSeconds = exam.getDuration() * 60L;
        long remainingSeconds = Math.max(0, totalSeconds - elapsedSeconds);
        // 还要受限于考试截止时间
        long secondsUntilEnd = Duration.between(now, exam.getEndTime()).getSeconds();
        remainingSeconds = Math.min(remainingSeconds, secondsUntilEnd);

        return ExamEnterVO.builder()
                .examId(examId)
                .examName(exam.getExamName())
                .paper(ExamEnterVO.PaperInfo.builder()
                        .id(paper.getId())
                        .paperName(paper.getPaperName())
                        .questions(questions)
                        .build())
                .remainingSeconds(remainingSeconds)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitExam(Long examId, ExamSubmitDTO submitDTO) {
        LoginUser loginUser = requireLoginUser();
        Long userId = loginUser.getUser().getId();

        ExamRecord record = examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getUserId, userId));

        if (record == null)
            throw new RuntimeException("考试记录不存在");
        if (record.getStatus() != 0)
            throw new RuntimeException("考试已提交");

        Exam exam = examMapper.selectById(examId);

        // 自动阅卷（仅限客观题，这里简化处理：假设所有题目对比标准答案）
        List<PaperQuestion> pqList = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, exam.getPaperId()));
        Map<Long, PaperQuestion> pqMap = pqList.stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, pq -> pq));

        List<Long> qIds = pqList.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> qMap = questionMapper.selectBatchIds(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        int totalScore = 0;
        for (ExamSubmitDTO.AnswerItem item : submitDTO.getAnswers()) {
            Question q = qMap.get(item.getQuestionId());
            PaperQuestion pq = pqMap.get(item.getQuestionId());
            if (q == null || pq == null)
                continue;

            boolean correct = q.getAnswer().trim().equalsIgnoreCase(item.getAnswer().trim());
            int score = correct ? pq.getScore() : 0;
            totalScore += score;

            AnswerDetail detail = AnswerDetail.builder()
                    .examRecordId(record.getId())
                    .questionId(item.getQuestionId())
                    .userAnswer(item.getAnswer())
                    .isCorrect(correct ? 1 : 0)
                    .score(score)
                    .timeSpent(item.getTimeSpent())
                    .build();
            answerDetailMapper.insert(detail);
        }

        record.setSubmitTime(LocalDateTime.now());
        record.setTotalScore(totalScore);
        record.setStatus(1); // 已交卷
        examRecordMapper.updateById(record);

        Map<String, Object> result = new HashMap<>();
        result.put("recordId", record.getId());
        result.put("totalScore", totalScore);
        result.put("status", 1);
        return result;
    }

    @Override
    public PageResult<StudentExamRecordVO> getStudentExamRecords(Integer page, Integer size) {
        LoginUser loginUser = requireLoginUser();
        Long userId = loginUser.getUser().getId();

        PageHelper.startPage(page, size);
        List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .orderByDesc(ExamRecord::getId));

        PageInfo<ExamRecord> pageInfo = new PageInfo<>(records);

        if (records.isEmpty()) {
            return PageResult.of(0L, new ArrayList<>());
        }

        // 获取考试和试卷信息以获取满分
        List<Long> examIds = records.stream().map(ExamRecord::getExamId).distinct().collect(Collectors.toList());
        List<Exam> exams = examMapper.selectBatchIds(examIds);
        Map<Long, String> examNameMap = exams.stream()
                .collect(Collectors.toMap(Exam::getId, Exam::getExamName));

        // 批量加载试卷信息
        Set<Long> paperIds = exams.stream().map(Exam::getPaperId).collect(Collectors.toSet());
        Map<Long, Integer> paperMaxScoreMap = paperIds.isEmpty() ? new HashMap<>()
                : paperMapper.selectBatchIds(paperIds).stream()
                        .collect(Collectors.toMap(Paper::getId,
                                p -> p.getTotalScore() != null ? p.getTotalScore() : 100));

        Map<Long, Integer> examMaxScoreMap = exams.stream()
                .collect(Collectors.toMap(Exam::getId, e -> paperMaxScoreMap.getOrDefault(e.getPaperId(), 100)));

        List<StudentExamRecordVO> voList = records.stream().map(r -> StudentExamRecordVO.builder()
                .id(r.getId())
                .examName(examNameMap.get(r.getExamId()))
                .totalScore(r.getTotalScore())
                .maxScore(examMaxScoreMap.get(r.getExamId()))
                .submitTime(r.getSubmitTime())
                .status(r.getStatus())
                .build()).collect(Collectors.toList());

        return PageResult.of(pageInfo.getTotal(), voList);
    }

    private void checkStudentExamPermission(Exam exam, Long userId, List<Long> classIds) {
        // 1. 基础校验：学生必须参加了该考试所属的课程
        Long enrolled = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, exam.getCourseId())
                .eq(CourseStudent::getStudentId, userId));
        if (enrolled == null || enrolled == 0) {
            throw new RuntimeException("您没有参加该课程，无权参加考试");
        }

        // 2. 检查分配情况
        Long classCount = examClassMapper.selectCount(new LambdaQueryWrapper<ExamClass>()
                .eq(ExamClass::getExamId, exam.getId()));
        Long studentCount = examStudentMapper.selectCount(new LambdaQueryWrapper<ExamStudent>()
                .eq(ExamStudent::getExamId, exam.getId()));

        // 如果既没分配班级也没分配个人，则全员开放
        if ((classCount == null || classCount == 0) && (studentCount == null || studentCount == 0)) {
            return;
        }

        // 检查班级关联
        if (classCount != null && classCount > 0 && classIds != null && !classIds.isEmpty()) {
            Long inClass = examClassMapper.selectCount(new LambdaQueryWrapper<ExamClass>()
                    .eq(ExamClass::getExamId, exam.getId())
                    .in(ExamClass::getClassId, classIds));
            if (inClass != null && inClass > 0)
                return;
        }

        // 检查学生直接指定
        if (studentCount != null && studentCount > 0) {
            Long isSpecified = examStudentMapper.selectCount(new LambdaQueryWrapper<ExamStudent>()
                    .eq(ExamStudent::getExamId, exam.getId())
                    .eq(ExamStudent::getStudentId, userId));
            if (isSpecified != null && isSpecified > 0)
                return;
        }

        throw new RuntimeException("您没有权限参加该考试");
    }

    private List<String> parseOptions(String optionsJson) {
        if (!StringUtils.hasText(optionsJson))
            return new ArrayList<>();
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public ExamRecordDetailVO getExamRecordDetail(Long recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("考试记录不存在");
        }

        LoginUser loginUser = requireLoginUser();
        // 只有学生本人或老师/管理员可以查看
        if (!loginUser.getRoleCodes().contains("ADMIN") && !loginUser.getRoleCodes().contains("TEACHER")
                && !Objects.equals(record.getUserId(), loginUser.getUser().getId())) {
            throw new RuntimeException("没有权限查看该考试记录");
        }

        Exam exam = examMapper.selectById(record.getExamId());
        Paper paper = paperMapper.selectById(exam.getPaperId());
        Integer maxScore = (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;

        // 查询答题详情
        List<AnswerDetail> details = answerDetailMapper.selectList(new LambdaQueryWrapper<AnswerDetail>()
                .eq(AnswerDetail::getExamRecordId, recordId));

        // 查询题目信息
        List<Long> questionIds = details.stream().map(AnswerDetail::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> questionMap = new HashMap<>();
        if (!questionIds.isEmpty()) {
            questionMap = questionMapper.selectBatchIds(questionIds).stream()
                    .collect(Collectors.toMap(Question::getId, q -> q));
        }

        // 查询题型信息用于显示
        List<QuestionType> types = questionTypeMapper.selectList(null);
        Map<Integer, String> typeNameMap = types.stream()
                .collect(Collectors.toMap(QuestionType::getId, QuestionType::getTypeName));

        // 计算排名
        int rank = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, record.getExamId())
                .ge(ExamRecord::getStatus, 1)
                .gt(ExamRecord::getTotalScore, record.getTotalScore())).intValue() + 1;
        int totalStudents = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, record.getExamId())
                .ge(ExamRecord::getStatus, 1)).intValue();

        // 计算知识点掌握度 (针对本次考试)
        List<ExamRecordDetailVO.KnowledgeMasteryVO> knowledgeMasteryList = new ArrayList<>();
        if (!questionIds.isEmpty()) {
            List<QuestionKnowledge> qks = questionKnowledgeMapper.selectList(new LambdaQueryWrapper<QuestionKnowledge>()
                    .in(QuestionKnowledge::getQuestionId, questionIds));

            Map<Long, List<Boolean>> kpResults = new HashMap<>();
            Map<Long, Boolean> qIdResultMap = details.stream()
                    .collect(Collectors.toMap(AnswerDetail::getQuestionId, d -> d.getIsCorrect() == 1, (a, b) -> a));

            for (QuestionKnowledge qk : qks) {
                Boolean result = qIdResultMap.get(qk.getQuestionId());
                if (result != null) {
                    kpResults.putIfAbsent(qk.getKnowledgePointId(), new ArrayList<>());
                    kpResults.get(qk.getKnowledgePointId()).add(result);
                }
            }

            if (!kpResults.isEmpty()) {
                Map<Long, String> kpNames = knowledgePointMapper.selectBatchIds(kpResults.keySet()).stream()
                        .collect(Collectors.toMap(KnowledgePoint::getId, KnowledgePoint::getName));

                kpResults.forEach((kpId, results) -> {
                    double acc = (double) results.stream().filter(b -> b).count() / results.size();
                    knowledgeMasteryList.add(new ExamRecordDetailVO.KnowledgeMasteryVO(kpNames.get(kpId), acc));
                });
            }
        }

        Map<Long, Question> finalQuestionMap = questionMap;
        List<ExamRecordDetailVO.AnswerItemVO> answers = details.stream().map(d -> {
            Question q = finalQuestionMap.get(d.getQuestionId());
            return ExamRecordDetailVO.AnswerItemVO.builder()
                    .questionId(d.getQuestionId())
                    .content(q != null ? q.getContent() : "题目已删除")
                    .userAnswer(d.getUserAnswer())
                    .correctAnswer(q != null ? q.getAnswer() : "-")
                    .score(d.getScore())
                    .isCorrect(d.getIsCorrect() == 1)
                    .analysis(q != null ? q.getAnalysis() : "-")
                    .options(q != null ? parseOptions(q.getOptions()) : new ArrayList<>())
                    .typeName(q != null ? typeNameMap.get(q.getTypeId()) : "-")
                    .build();
        }).collect(Collectors.toList());

        return ExamRecordDetailVO.builder()
                .examName(exam.getExamName())
                .totalScore(record.getTotalScore())
                .maxScore(maxScore)
                .rank(rank)
                .totalStudents(totalStudents)
                .knowledgeMastery(knowledgeMasteryList)
                .answers(answers)
                .build();
    }

    private ExamVO toExamVO(Exam e) {
        Integer status = e.getStatus();
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(e.getStartTime())) {
            status = 0;
        } else if (now.isAfter(e.getEndTime())) {
            status = 2;
        } else {
            status = 1;
        }

        return ExamVO.builder()
                .id(e.getId())
                .examName(e.getExamName())
                .paperId(e.getPaperId())
                .courseId(e.getCourseId())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .duration(e.getDuration())
                .status(status)
                .publishScore(e.getPublishScore().intValue())
                .createBy(e.getCreateBy())
                .createTime(e.getCreateTime())
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
}
