package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.zaixiantiku.entity.*;
import com.example.zaixiantiku.mapper.*;
import com.example.zaixiantiku.pojo.dto.CheatRecordDTO;
import com.example.zaixiantiku.pojo.dto.ExamSubmitDTO;
import com.example.zaixiantiku.pojo.vo.*;
import com.example.zaixiantiku.security.LoginUser;
import com.example.zaixiantiku.service.StudentExamService;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    private final UserMapper userMapper;
    private final LogMapper logMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void recordCheat(CheatRecordDTO cheatDTO) {
        LoginUser loginUser = requireLoginUser();
        Long userId = loginUser.getUser().getId();

        try {
            Log log = Log.builder()
                    .userId(userId)
                    .operation("切屏")
                    .module("考试")
                    .params(objectMapper.writeValueAsString(cheatDTO))
                    .createTime(LocalDateTime.now())
                    .build();
            logMapper.insert(log);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PageResult<ExamVO> getStudentExams(Long courseId) {
        LoginUser loginUser = requireLoginUser();
        Long userId = loginUser.getUser().getId();

        // 1. 获取学生选修的所有课程ID
        List<Long> enrolledCourseIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getStudentId, userId))
                .stream().map(CourseStudent::getCourseId).collect(Collectors.toList());

        if (enrolledCourseIds.isEmpty()) {
            return PageResult.of(0L, new ArrayList<>());
        }

        // 2. 查询学生选修课程下的所有考试
        // 状态说明：0-未开始，1-进行中，2-已结束。学生应能看到未开始和进行中的。
        LambdaQueryWrapper<Exam> examQw = new LambdaQueryWrapper<Exam>()
                .ne(Exam::getStatus, 2) // 不显示已结束的考试（已结束的在考试记录中查看）
                .in(Exam::getCourseId, enrolledCourseIds);

        if (courseId != null) {
            if (!enrolledCourseIds.contains(courseId)) {
                throw new RuntimeException("您未选修该课程，无权查看其考试");
            }
            examQw.eq(Exam::getCourseId, courseId);
        }

        List<Exam> allExamsInCourses = examMapper.selectList(examQw);
        if (allExamsInCourses.isEmpty()) {
            return PageResult.of(0L, new ArrayList<>());
        }

        // 3. 过滤出该学生有权参加的考试（班级关联或个人关联）
        // 获取该学生所属的所有班级ID
        List<Long> classIds = studentClassMapper.selectList(new LambdaQueryWrapper<StudentClass>()
                .eq(StudentClass::getStudentId, userId))
                .stream().map(StudentClass::getClassId).collect(Collectors.toList());

        List<Exam> exams = allExamsInCourses.stream().filter(exam -> {
            try {
                // 复用权限校验逻辑进行过滤，但不抛出异常
                return hasStudentExamPermission(exam, userId, classIds);
            } catch (Exception e) {
                return false;
            }
        }).collect(Collectors.toList());

        if (exams.isEmpty()) {
            return PageResult.of(0L, new ArrayList<>());
        }

        // 4. 获取学生已提交的考试记录，用于过滤
        List<Long> allExamIds = exams.stream().map(Exam::getId).collect(Collectors.toList());
        List<ExamRecord> submittedRecords = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .in(ExamRecord::getExamId, allExamIds)
                .ge(ExamRecord::getStatus, 1)); // 状态 >= 1 表示已提交或已批阅

        Set<Long> submittedExamIds = submittedRecords.stream().map(ExamRecord::getExamId).collect(Collectors.toSet());

        // 仅显示未提交的考试
        List<Exam> unsubmittedExams = exams.stream()
                .filter(e -> !submittedExamIds.contains(e.getId()))
                .collect(Collectors.toList());

        if (unsubmittedExams.isEmpty()) {
            return PageResult.of(0L, new ArrayList<>());
        }

        // 5. 获取试卷名映射
        List<Long> paperIds = unsubmittedExams.stream().map(Exam::getPaperId).distinct().collect(Collectors.toList());
        Map<Long, String> paperNameMap = new HashMap<>();
        if (!paperIds.isEmpty()) {
            paperNameMap = paperMapper.selectBatchIds(paperIds).stream()
                    .collect(Collectors.toMap(Paper::getId, Paper::getPaperName));
        }

        Map<Long, String> finalPaperNameMap = paperNameMap;
        List<ExamVO> voList = unsubmittedExams.stream().map(e -> {
            ExamVO vo = toExamVO(e, false); // 这里一定是未提交的
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

        // 加载已保存的答案
        List<AnswerDetail> savedDetails = answerDetailMapper.selectList(new LambdaQueryWrapper<AnswerDetail>()
                .eq(AnswerDetail::getExamRecordId, record.getId()));
        Map<Long, String> savedAnswers = savedDetails.stream()
                .collect(Collectors.toMap(AnswerDetail::getQuestionId, AnswerDetail::getUserAnswer, (v1, v2) -> v1));

        // 从日志统计切屏次数
        Long cheatCount = 0L;
        try {
            String examIdStr = "\"examId\":" + examId;
            cheatCount = logMapper.selectCount(new LambdaQueryWrapper<Log>()
                    .eq(Log::getUserId, userId)
                    .eq(Log::getOperation, "切屏")
                    .like(Log::getParams, examIdStr));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ExamEnterVO.builder()
                .examId(examId)
                .examName(exam.getExamName())
                .paper(ExamEnterVO.PaperInfo.builder()
                        .id(paper.getId())
                        .paperName(paper.getPaperName())
                        .questions(questions)
                        .build())
                .remainingSeconds(remainingSeconds)
                .answers(savedAnswers)
                .cheatCount(cheatCount.intValue())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAnswer(Long examId, Long questionId, String answer) {
        LoginUser loginUser = requireLoginUser();
        Long userId = loginUser.getUser().getId();

        // 获取当前正在进行中的考试记录
        ExamRecord record = examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getUserId, userId)
                .eq(ExamRecord::getStatus, 0));

        if (record == null) {
            throw new RuntimeException("考试已结束或记录不存在");
        }

        // 更新或插入答案详情
        AnswerDetail detail = answerDetailMapper.selectOne(new LambdaQueryWrapper<AnswerDetail>()
                .eq(AnswerDetail::getExamRecordId, record.getId())
                .eq(AnswerDetail::getQuestionId, questionId));

        if (detail == null) {
            detail = AnswerDetail.builder()
                    .examRecordId(record.getId())
                    .questionId(questionId)
                    .userAnswer(answer)
                    .build();
            answerDetailMapper.insert(detail);
        } else {
            detail.setUserAnswer(answer);
            answerDetailMapper.updateById(detail);
        }
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

        // 获取题目类型信息，区分客观题与主观题
        List<QuestionType> typeList = questionTypeMapper.selectList(null);
        Set<Integer> objectiveTypeIds = typeList.stream()
                .filter(t -> {
                    String code = (t.getTypeCode() != null ? t.getTypeCode() : "").toUpperCase();
                    String name = (t.getTypeName() != null ? t.getTypeName() : "");
                    // 客观题：单选、多选、判断
                    return code.contains("SINGLE") || name.contains("单选") ||
                            code.contains("MULTI") || name.contains("多选") ||
                            code.contains("JUDGE") || name.contains("判断");
                })
                .map(QuestionType::getId)
                .collect(Collectors.toSet());

        List<PaperQuestion> pqList = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, exam.getPaperId()));
        Map<Long, PaperQuestion> pqMap = pqList.stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, pq -> pq));

        List<Long> qIds = pqList.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> qMap = questionMapper.selectBatchIds(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 识别多选题类型 ID
        Set<Integer> multiChoiceTypeIds = typeList.stream()
                .filter(t -> {
                    String code = (t.getTypeCode() != null ? t.getTypeCode() : "").toUpperCase();
                    String name = (t.getTypeName() != null ? t.getTypeName() : "");
                    return code.contains("MULTI") || name.contains("多选");
                })
                .map(QuestionType::getId)
                .collect(Collectors.toSet());

        int objectiveScore = 0;
        boolean hasSubjective = false;

        for (ExamSubmitDTO.AnswerItem item : submitDTO.getAnswers()) {
            Question q = qMap.get(item.getQuestionId());
            PaperQuestion pq = pqMap.get(item.getQuestionId());
            if (q == null || pq == null)
                continue;

            boolean isObjective = objectiveTypeIds.contains(q.getTypeId());
            int score = 0;
            Integer isCorrect = null;

            if (isObjective) {
                // 客观题自动阅卷
                String standardAnswer = q.getAnswer() != null ? q.getAnswer().trim() : "";
                String studentAnswer = item.getAnswer() != null ? item.getAnswer().trim() : "";

                boolean correct;
                // 对于多选题，进行特殊处理：去除逗号等分隔符后排序比较
                if (q.getTypeId() != null && multiChoiceTypeIds.contains(q.getTypeId())) {
                    standardAnswer = standardAnswer.replaceAll("[^A-Z0-9]", "");
                    studentAnswer = studentAnswer.replaceAll("[^A-Z0-9]", "");

                    char[] sArr = standardAnswer.toCharArray();
                    Arrays.sort(sArr);
                    standardAnswer = new String(sArr);

                    char[] uArr = studentAnswer.toCharArray();
                    Arrays.sort(uArr);
                    studentAnswer = new String(uArr);

                    correct = !standardAnswer.isEmpty() && standardAnswer.equalsIgnoreCase(studentAnswer);
                } else {
                    correct = standardAnswer.equalsIgnoreCase(studentAnswer);
                }

                score = correct ? pq.getScore() : 0;
                objectiveScore += score;
                isCorrect = correct ? 1 : 0;
            } else {
                // 主观题暂不计分，等待人工批阅
                hasSubjective = true;
                score = 0;
                isCorrect = 0; // 初始为0，由老师批阅后修正
            }

            // 更新或插入答案详情
            AnswerDetail detail = answerDetailMapper.selectOne(new LambdaQueryWrapper<AnswerDetail>()
                    .eq(AnswerDetail::getExamRecordId, record.getId())
                    .eq(AnswerDetail::getQuestionId, item.getQuestionId()));

            if (detail == null) {
                detail = AnswerDetail.builder()
                        .examRecordId(record.getId())
                        .questionId(item.getQuestionId())
                        .userAnswer(item.getAnswer())
                        .isCorrect(isCorrect)
                        .score(score)
                        .timeSpent(item.getTimeSpent())
                        .build();
                answerDetailMapper.insert(detail);
            } else {
                detail.setUserAnswer(item.getAnswer());
                detail.setIsCorrect(isCorrect);
                detail.setScore(score);
                detail.setTimeSpent(item.getTimeSpent());
                answerDetailMapper.updateById(detail);
            }
        }

        record.setSubmitTime(LocalDateTime.now());
        record.setTotalScore(objectiveScore);
        // 如果没有主观题，直接标记为已批阅(2)；否则标记为待批阅(1)
        record.setStatus(hasSubjective ? 1 : 2);

        // 记录交卷日志（包含防作弊元数据，替代缺失的 exam_record.answers 字段）
        try {
            Map<String, Object> meta = new HashMap<>();
            meta.put("examId", examId);
            meta.put("cheatCount", submitDTO.getCheatCount());
            meta.put("forceSubmit", submitDTO.getForceSubmit());

            Log submitLog = Log.builder()
                    .userId(userId)
                    .operation(submitDTO.getForceSubmit() ? "强制交卷" : "正常交卷")
                    .module("考试")
                    .params(objectMapper.writeValueAsString(meta))
                    .createTime(LocalDateTime.now())
                    .build();
            logMapper.insert(submitLog);
        } catch (Exception e) {
            e.printStackTrace();
        }

        examRecordMapper.updateById(record);

        Map<String, Object> result = new HashMap<>();
        result.put("recordId", record.getId());
        result.put("totalScore", objectiveScore);
        result.put("status", hasSubjective ? 1 : 2);
        return result;
    }

    @Override
    public PageResult<StudentExamRecordVO> getStudentExamRecords(Integer page, Integer size, Long courseId) {
        LoginUser loginUser = requireLoginUser();
        Long userId = loginUser.getUser().getId();

        // 1. 获取学生选修的所有课程ID
        List<Long> enrolledCourseIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getStudentId, userId))
                .stream().map(CourseStudent::getCourseId).collect(Collectors.toList());

        if (enrolledCourseIds.isEmpty()) {
            return PageResult.of(0L, new ArrayList<>());
        }

        LambdaQueryWrapper<ExamRecord> qw = new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .ge(ExamRecord::getStatus, 1); // 仅显示已提交或已批阅的记录

        // 2. 根据选修课程ID获取所有相关考试ID
        List<Long> examIdsInEnrolledCourses = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                .in(Exam::getCourseId, enrolledCourseIds))
                .stream().map(Exam::getId).collect(Collectors.toList());

        if (examIdsInEnrolledCourses.isEmpty()) {
            return PageResult.of(0L, new ArrayList<>());
        }
        qw.in(ExamRecord::getExamId, examIdsInEnrolledCourses);

        // 如果指定了courseId，则进一步过滤
        if (courseId != null) {
            // 验证指定的courseId是否在学生已选课程中
            if (!enrolledCourseIds.contains(courseId)) {
                throw new RuntimeException("您未选修该课程，无权查看其考试记录");
            }
            // 进一步过滤，只包含指定课程的考试ID
            List<Long> specificCourseExamIds = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                    .eq(Exam::getCourseId, courseId))
                    .stream().map(Exam::getId).collect(Collectors.toList());
            if (specificCourseExamIds.isEmpty()) {
                return PageResult.of(0L, new ArrayList<>());
            }
            qw.in(ExamRecord::getExamId, specificCourseExamIds);
        }

        PageHelper.startPage(page, size);
        qw.orderByDesc(ExamRecord::getId);
        List<ExamRecord> records = examRecordMapper.selectList(qw);

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

        List<StudentExamRecordVO> voList = records.stream().map(r -> {
            boolean isMarked = r.getStatus() == 2;
            return StudentExamRecordVO.builder()
                    .id(r.getId())
                    .examName(examNameMap.get(r.getExamId()))
                    .totalScore(isMarked ? r.getTotalScore() : null) // 待批阅时不显示总分
                    .maxScore(examMaxScoreMap.get(r.getExamId()))
                    .submitTime(r.getSubmitTime())
                    .status(r.getStatus())
                    .build();
        }).collect(Collectors.toList());

        return PageResult.of(pageInfo.getTotal(), voList);
    }

    private void checkStudentExamPermission(Exam exam, Long userId, List<Long> classIds) {
        if (!hasStudentExamPermission(exam, userId, classIds)) {
            throw new RuntimeException("您没有权限参加该考试");
        }
    }

    private boolean hasStudentExamPermission(Exam exam, Long userId, List<Long> classIds) {
        // 1. 基础校验：学生必须参加了该考试所属的课程
        Long enrolled = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, exam.getCourseId())
                .eq(CourseStudent::getStudentId, userId));
        if (enrolled == null || enrolled == 0) {
            return false;
        }

        // 2. 检查分配情况
        Long classCount = examClassMapper.selectCount(new LambdaQueryWrapper<ExamClass>()
                .eq(ExamClass::getExamId, exam.getId()));
        Long studentCount = examStudentMapper.selectCount(new LambdaQueryWrapper<ExamStudent>()
                .eq(ExamStudent::getExamId, exam.getId()));

        // 如果既没分配班级也没分配个人，则全员开放
        if ((classCount == null || classCount == 0) && (studentCount == null || studentCount == 0)) {
            return true;
        }

        // 检查班级关联
        if (classCount != null && classCount > 0 && classIds != null && !classIds.isEmpty()) {
            Long inClass = examClassMapper.selectCount(new LambdaQueryWrapper<ExamClass>()
                    .eq(ExamClass::getExamId, exam.getId())
                    .in(ExamClass::getClassId, classIds));
            if (inClass != null && inClass > 0)
                return true;
        }

        // 检查学生直接指定
        if (studentCount != null && studentCount > 0) {
            Long isSpecified = examStudentMapper.selectCount(new LambdaQueryWrapper<ExamStudent>()
                    .eq(ExamStudent::getExamId, exam.getId())
                    .eq(ExamStudent::getStudentId, userId));
            if (isSpecified != null && isSpecified > 0)
                return true;
        }

        return false;
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

        // 查询试卷题目分值信息
        List<PaperQuestion> pqList = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, exam.getPaperId()));
        Map<Long, Integer> questionScoreMap = pqList.stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, PaperQuestion::getScore));

        // 查询题型信息
        List<QuestionType> types = questionTypeMapper.selectList(null);
        Map<Integer, String> typeNameMap = types.stream()
                .collect(Collectors.toMap(QuestionType::getId, QuestionType::getTypeName));

        Set<Integer> objectiveTypeIds = types.stream()
                .filter(t -> {
                    String code = (t.getTypeCode() != null ? t.getTypeCode() : "").toUpperCase();
                    String name = (t.getTypeName() != null ? t.getTypeName() : "");
                    return code.contains("SINGLE") || name.contains("单选") ||
                            code.contains("MULTI") || name.contains("多选") ||
                            code.contains("JUDGE") || name.contains("判断");
                })
                .map(QuestionType::getId)
                .collect(Collectors.toSet());

        Set<Integer> multiChoiceTypeIds = types.stream()
                .filter(t -> {
                    String code = (t.getTypeCode() != null ? t.getTypeCode() : "").toUpperCase();
                    String name = (t.getTypeName() != null ? t.getTypeName() : "");
                    return code.contains("MULTI") || name.contains("多选");
                })
                .map(QuestionType::getId)
                .collect(Collectors.toSet());

        // 重新判定客观题并更新分数（用于修复旧记录判分错误的问题）
        boolean scoreChanged = false;
        int totalObjectiveScore = 0;
        int totalSubjectiveScore = 0;

        for (AnswerDetail detail : details) {
            Question q = questionMap.get(detail.getQuestionId());
            if (q == null)
                continue;

            boolean isObjective = objectiveTypeIds.contains(q.getTypeId());
            if (isObjective) {
                String standardAnswer = q.getAnswer() != null ? q.getAnswer().trim() : "";
                String studentAnswer = detail.getUserAnswer() != null ? detail.getUserAnswer().trim() : "";
                Integer pqScore = questionScoreMap.get(q.getId());
                if (pqScore == null)
                    pqScore = 0;

                boolean correct;
                if (q.getTypeId() != null && multiChoiceTypeIds.contains(q.getTypeId())) {
                    String sAns = standardAnswer.replaceAll("[^A-Z0-9]", "");
                    String uAns = studentAnswer.replaceAll("[^A-Z0-9]", "");
                    char[] sArr = sAns.toCharArray();
                    Arrays.sort(sArr);
                    sAns = new String(sArr);
                    char[] uArr = uAns.toCharArray();
                    Arrays.sort(uArr);
                    uAns = new String(uArr);
                    correct = !sAns.isEmpty() && sAns.equalsIgnoreCase(uAns);
                } else {
                    correct = standardAnswer.equalsIgnoreCase(studentAnswer);
                }

                int newScore = correct ? pqScore : 0;
                int newIsCorrect = correct ? 1 : 0;

                if (!Objects.equals(detail.getScore(), newScore)
                        || !Objects.equals(detail.getIsCorrect(), newIsCorrect)) {
                    detail.setScore(newScore);
                    detail.setIsCorrect(newIsCorrect);
                    answerDetailMapper.updateById(detail);
                    scoreChanged = true;
                }
                totalObjectiveScore += newScore;
            } else {
                totalSubjectiveScore += (detail.getScore() != null ? detail.getScore() : 0);
            }
        }

        if (scoreChanged) {
            record.setTotalScore(totalObjectiveScore + totalSubjectiveScore);
            examRecordMapper.updateById(record);
        }

        // 重新计算排名和总分显示
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
                    .maxScore(q != null ? questionScoreMap.get(q.getId()) : 0)
                    .isCorrect(d.getIsCorrect() == 1)
                    .analysis(q != null ? q.getAnalysis() : "-")
                    .options(q != null ? parseOptions(q.getOptions()) : new ArrayList<>())
                    .typeName(q != null ? typeNameMap.get(q.getTypeId()) : "-")
                    .build();
        }).collect(Collectors.toList());

        // 如果未批阅且不是老师/管理员，隐藏总分和部分详情
        boolean isMarked = record.getStatus() == 2;
        boolean canSeeFull = isMarked || loginUser.getRoleCodes().contains("ADMIN")
                || loginUser.getRoleCodes().contains("TEACHER");

        // 查询学生姓名
        User student = userMapper.selectById(record.getUserId());
        String studentName = student != null ? (student.getName() != null ? student.getName() : student.getUsername())
                : "未知学生";

        return ExamRecordDetailVO.builder()
                .studentName(studentName)
                .examName(exam.getExamName())
                .totalScore(canSeeFull ? record.getTotalScore() : null)
                .maxScore(maxScore)
                .rank(canSeeFull ? rank : null)
                .totalStudents(totalStudents)
                .knowledgeMastery(canSeeFull ? knowledgeMasteryList : new ArrayList<>())
                .answers(canSeeFull ? answers : new ArrayList<>())
                .status(record.getStatus())
                .build();
    }

    private ExamVO toExamVO(Exam e, boolean isSubmitted) {
        Integer studentStatus;
        LocalDateTime now = LocalDateTime.now();

        if (isSubmitted) {
            studentStatus = 3; // 已提交
        } else if (now.isBefore(e.getStartTime())) {
            studentStatus = 0; // 未开始
        } else if (now.isAfter(e.getEndTime())) {
            studentStatus = 2; // 已结束
        } else {
            studentStatus = 1; // 进行中
        }

        return ExamVO.builder()
                .id(e.getId())
                .examName(e.getExamName())
                .paperId(e.getPaperId())
                .courseId(e.getCourseId())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .duration(e.getDuration())
                .status(studentStatus) // 兼容前端，将学生状态传给status
                .studentStatus(studentStatus)
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
