package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.zaixiantiku.entity.*;
import com.example.zaixiantiku.mapper.*;
import com.example.zaixiantiku.pojo.vo.*;
import com.example.zaixiantiku.security.LoginUser;
import com.example.zaixiantiku.service.AnalysisService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final PracticeRecordMapper practiceRecordMapper;
    private final MistakeBookMapper mistakeBookMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ExamRecordMapper examRecordMapper;
    private final AnswerDetailMapper answerDetailMapper;
    private final StudentClassMapper studentClassMapper;
    private final QuestionMapper questionMapper;
    private final ClassMapper classMapper;
    private final ExamMapper examMapper;
    private final ExamClassMapper examClassMapper;
    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionTypeMapper questionTypeMapper;
    private final CourseTeacherMapper courseTeacherMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final LogMapper logMapper;
    private final ObjectMapper objectMapper;

    private boolean checkCorrect(Object isCorrectObj) {
        if (isCorrectObj == null)
            return false;
        if (isCorrectObj instanceof Boolean) {
            return (Boolean) isCorrectObj;
        }
        if (isCorrectObj instanceof Number) {
            return ((Number) isCorrectObj).intValue() == 1;
        }
        if (isCorrectObj instanceof String) {
            String s = (String) isCorrectObj;
            return "true".equalsIgnoreCase(s) || "1".equals(s);
        }
        return false;
    }

    @Override
    public StudentAnalysisVO getStudentAnalysisReport(Long courseId, String timeRange) {
        if (courseId == null) {
            return StudentAnalysisVO.builder()
                    .totalPracticeCount(0).totalPracticeQuestions(0).avgPracticeAccuracy(0.0)
                    .totalExamCount(0).avgExamScoreRate(0.0).maxExamScore(0).maxExamTotalScore(0)
                    .mistakeCount(0).practiceTrend(new ArrayList<>()).examTypeStats(new ArrayList<>())
                    .knowledgeRadar(new ArrayList<>()).build();
        }

        Long userId = getUserId();
        // 权限校验：学生必须选修了该课程
        Long enrollCount = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getStudentId, userId)
                .eq(CourseStudent::getCourseId, courseId));
        if (enrollCount == null || enrollCount == 0) {
            throw new RuntimeException("您未选修该课程，无权查看分析报告");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime startTime = null;

        if ("week".equals(timeRange)) {
            startTime = now.minusDays(7);
        } else if ("month".equals(timeRange)) {
            startTime = now.minusDays(30);
        }

        // --- 1. 获取练习概况 ---
        LambdaQueryWrapper<PracticeRecord> practiceQw = new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getCourseId, courseId)
                .isNotNull(PracticeRecord::getSubmitTime);
        if (startTime != null)
            practiceQw.ge(PracticeRecord::getSubmitTime, startTime);

        List<PracticeRecord> practiceRecords = practiceRecordMapper.selectList(practiceQw);

        int totalPracticeQuestions = 0;
        int totalPracticeCorrect = 0;
        Map<String, List<Boolean>> practiceTrendMap = new TreeMap<>();
        Map<Long, List<Boolean>> practiceQIdResults = new HashMap<>();

        for (PracticeRecord record : practiceRecords) {
            String date = record.getSubmitTime().format(formatter);
            practiceTrendMap.putIfAbsent(date, new ArrayList<>());

            List<Map<String, Object>> answers = record.getAnswers();
            if (answers != null && !answers.isEmpty()) {
                for (Map<String, Object> ans : answers) {
                    if (ans.get("questionId") == null)
                        continue;
                    Long qId = Long.valueOf(ans.get("questionId").toString());
                    boolean isCorrect = checkCorrect(ans.get("isCorrect"));

                    totalPracticeQuestions++;
                    if (isCorrect)
                        totalPracticeCorrect++;
                    practiceTrendMap.get(date).add(isCorrect);
                    practiceQIdResults.putIfAbsent(qId, new ArrayList<>());
                    practiceQIdResults.get(qId).add(isCorrect);
                }
            }
        }

        // --- 2. 获取考试概况 ---
        LambdaQueryWrapper<ExamRecord> examRecordQw = new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .ge(ExamRecord::getStatus, 1); // 状态 >= 1 表示已提交或已批阅

        // 获取该课程下的所有考试ID
        List<Long> examIdsInCourse = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                .eq(Exam::getCourseId, courseId))
                .stream().map(Exam::getId).collect(Collectors.toList());

        if (examIdsInCourse.isEmpty()) {
            // 如果该课程下没有考试，则直接返回空数据，避免后续查询
            return StudentAnalysisVO.builder()
                    .totalPracticeCount(0).totalPracticeQuestions(0).avgPracticeAccuracy(0.0)
                    .totalExamCount(0).avgExamScoreRate(0.0).maxExamScore(0).maxExamTotalScore(0)
                    .mistakeCount(0).practiceTrend(new ArrayList<>()).examTypeStats(new ArrayList<>())
                    .knowledgeRadar(new ArrayList<>()).build();
        }
        examRecordQw.in(ExamRecord::getExamId, examIdsInCourse);

        if (startTime != null)
            examRecordQw.ge(ExamRecord::getSubmitTime, startTime);

        List<ExamRecord> courseExamRecords = examRecordMapper.selectList(examRecordQw);

        int totalExamCount = courseExamRecords.size();
        int maxExamScore = 0;
        int maxExamTotalScore = 0;
        double totalExamScoreSum = 0;
        double totalMaxScoreSum = 0;
        Map<Integer, List<Boolean>> typeResults = new HashMap<>();

        // 批量获取所有相关考试和试卷信息，避免循环查询
        Map<Long, Exam> examMap = new HashMap<>();
        Map<Long, Paper> paperMap = new HashMap<>();
        if (!courseExamRecords.isEmpty()) {
            Set<Long> recordExamIds = courseExamRecords.stream().map(ExamRecord::getExamId).collect(Collectors.toSet());
            examMap = examMapper.selectBatchIds(recordExamIds).stream()
                    .collect(Collectors.toMap(Exam::getId, e -> e));
            Set<Long> paperIds = examMap.values().stream().map(Exam::getPaperId).collect(Collectors.toSet());
            if (!paperIds.isEmpty()) {
                paperMap = paperMapper.selectBatchIds(paperIds).stream()
                        .collect(Collectors.toMap(Paper::getId, p -> p));
            }
        }

        for (ExamRecord er : courseExamRecords) {
            int score = (er.getTotalScore() != null) ? er.getTotalScore() : 0;
            totalExamScoreSum += score;

            Exam exam = examMap.get(er.getExamId());
            int maxScore = 100;
            if (exam != null) {
                Paper paper = paperMap.get(exam.getPaperId());
                maxScore = (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;
                totalMaxScoreSum += maxScore;

                if (score > maxExamScore || (maxExamScore == 0 && maxExamTotalScore == 0)) {
                    maxExamScore = score;
                    maxExamTotalScore = maxScore;
                }
            }

            // 获取考试题目详情以进行题型分布统计
            List<AnswerDetail> details = answerDetailMapper.selectList(
                    new LambdaQueryWrapper<AnswerDetail>().eq(AnswerDetail::getExamRecordId, er.getId()));
            if (!details.isEmpty()) {
                List<Long> qIds = details.stream().map(AnswerDetail::getQuestionId).collect(Collectors.toList());
                Map<Long, Question> qMap = questionMapper.selectBatchIds(qIds).stream()
                        .collect(Collectors.toMap(Question::getId, q -> q));
                for (AnswerDetail detail : details) {
                    Question q = qMap.get(detail.getQuestionId());
                    if (q != null) {
                        typeResults.putIfAbsent(q.getTypeId(), new ArrayList<>());
                        typeResults.get(q.getTypeId()).add(detail.getIsCorrect() == 1);
                    }
                }
            }
        }

        // --- 3. 错题统计 ---
        LambdaQueryWrapper<MistakeBook> mistakeQw = new LambdaQueryWrapper<MistakeBook>()
                .eq(MistakeBook::getUserId, userId)
                .eq(MistakeBook::getCourseId, courseId);
        int mistakeCount = mistakeBookMapper.selectCount(mistakeQw).intValue();

        // --- 4. 统计题型分布 (基于考试数据 + 练习数据) ---
        // 之前已经有了考试数据的 typeResults (Map<Integer, List<Boolean>>)
        // 现在加入练习数据 (practiceQIdResults: Map<Long, List<Boolean>>)
        if (!practiceQIdResults.isEmpty()) {
            List<Question> practiceQuestions = questionMapper.selectBatchIds(practiceQIdResults.keySet());
            for (Question q : practiceQuestions) {
                List<Boolean> results = practiceQIdResults.get(q.getId());
                if (results != null) {
                    typeResults.putIfAbsent(q.getTypeId(), new ArrayList<>());
                    typeResults.get(q.getTypeId()).addAll(results);
                }
            }
        }

        List<StudentAnalysisVO.TypeStatVO> examTypeStats = new ArrayList<>();
        if (!typeResults.isEmpty()) {
            Map<Integer, String> typeNames = questionTypeMapper.selectBatchIds(typeResults.keySet()).stream()
                    .collect(Collectors.toMap(QuestionType::getId, QuestionType::getTypeName));
            typeResults.forEach((typeId, results) -> {
                long correct = results.stream().filter(b -> b).count();
                double acc = (double) correct / results.size();
                examTypeStats.add(
                        new StudentAnalysisVO.TypeStatVO(typeNames.get(typeId), acc));
            });
        }

        // --- 5. 统计知识点掌握情况 (基于练习数据) ---
        List<StudentAnalysisVO.KnowledgeRadarVO> knowledgeRadar = new ArrayList<>();
        if (!practiceQIdResults.isEmpty()) {
            List<QuestionKnowledge> qks = questionKnowledgeMapper.selectList(
                    new LambdaQueryWrapper<QuestionKnowledge>().in(QuestionKnowledge::getQuestionId,
                            practiceQIdResults.keySet()));
            Map<Long, List<Boolean>> kpResults = new HashMap<>();
            for (QuestionKnowledge qk : qks) {
                List<Boolean> results = practiceQIdResults.get(qk.getQuestionId());
                if (results != null) {
                    kpResults.putIfAbsent(qk.getKnowledgePointId(), new ArrayList<>());
                    kpResults.get(qk.getKnowledgePointId()).addAll(results);
                }
            }
            if (!kpResults.isEmpty()) {
                Map<Long, String> kpNames = knowledgePointMapper.selectBatchIds(kpResults.keySet()).stream()
                        .collect(Collectors.toMap(KnowledgePoint::getId, KnowledgePoint::getName));
                kpResults.forEach((kpId, results) -> {
                    double acc = (double) results.stream().filter(b -> b).count() / results.size();
                    knowledgeRadar
                            .add(new StudentAnalysisVO.KnowledgeRadarVO(kpNames.get(kpId), acc));
                });
            }
        }

        // --- 6. 学习正确率趋势 (近7天) ---
        List<StudentAnalysisVO.TrendVO> practiceTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String date = now.minusDays(i).format(formatter);
            List<Boolean> results = practiceTrendMap.get(date);
            double acc = 0.0;
            if (results != null && !results.isEmpty()) {
                acc = (double) results.stream().filter(b -> b).count() / results.size();
            }
            practiceTrend.add(new StudentAnalysisVO.TrendVO(date, acc));
        }

        return StudentAnalysisVO.builder()
                .totalPracticeCount(practiceRecords.size())
                .totalPracticeQuestions(totalPracticeQuestions)
                .avgPracticeAccuracy(
                        totalPracticeQuestions == 0 ? 0.0 : (double) totalPracticeCorrect / totalPracticeQuestions)
                .totalExamCount(totalExamCount)
                .avgExamScoreRate(totalMaxScoreSum == 0 ? 0.0 : totalExamScoreSum / totalMaxScoreSum)
                .maxExamScore(maxExamScore)
                .maxExamTotalScore(maxExamTotalScore)
                .mistakeCount(mistakeCount)
                .practiceTrend(practiceTrend)
                .examTypeStats(examTypeStats)
                .knowledgeRadar(knowledgeRadar)
                .build();
    }

    @Override
    public ClassAnalysisVO getClassAnalysis(Long classId, Long examId, Long courseId) {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isAdmin = loginUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        // 1. 获取班级所有学生
        List<StudentClass> studentsInClass = studentClassMapper.selectList(
                new LambdaQueryWrapper<StudentClass>().eq(StudentClass::getClassId, classId));
        List<Long> studentIds = studentsInClass.stream().map(StudentClass::getStudentId).collect(Collectors.toList());

        if (studentIds.isEmpty()) {
            return ClassAnalysisVO.builder()
                    .scoreDistribution(Collections.emptyList())
                    .averageScore(0.0)
                    .questionAccuracies(Collections.emptyList())
                    .build();
        }

        // 2. 获取考试记录
        LambdaQueryWrapper<ExamRecord> recordQw = new LambdaQueryWrapper<ExamRecord>()
                .in(ExamRecord::getUserId, studentIds)
                .ge(ExamRecord::getStatus, 1);

        if (!isAdmin) {
            List<Long> teacherCourseIds = courseTeacherMapper.selectList(new LambdaQueryWrapper<CourseTeacher>()
                    .eq(CourseTeacher::getTeacherId, loginUser.getUser().getId()))
                    .stream().map(CourseTeacher::getCourseId).collect(Collectors.toList());

            if (teacherCourseIds.isEmpty()) {
                return ClassAnalysisVO.builder().scoreDistribution(Collections.emptyList()).averageScore(0.0)
                        .questionAccuracies(Collections.emptyList()).build();
            }

            if (courseId != null && !teacherCourseIds.contains(courseId)) {
                throw new RuntimeException("您无权查看该课程的分析数据");
            }

            if (examId != null) {
                Exam exam = examMapper.selectById(examId);
                if (exam == null || !teacherCourseIds.contains(exam.getCourseId())) {
                    return ClassAnalysisVO.builder().scoreDistribution(Collections.emptyList()).averageScore(0.0)
                            .questionAccuracies(Collections.emptyList()).build();
                }
                recordQw.eq(ExamRecord::getExamId, examId);
            } else if (courseId != null) {
                List<Long> courseExamIds = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                        .eq(Exam::getCourseId, courseId))
                        .stream().map(Exam::getId).collect(Collectors.toList());
                if (courseExamIds.isEmpty()) {
                    return ClassAnalysisVO.builder().scoreDistribution(Collections.emptyList()).averageScore(0.0)
                            .questionAccuracies(Collections.emptyList()).build();
                }
                recordQw.in(ExamRecord::getExamId, courseExamIds);
            } else {
                List<Long> teacherExamIds = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                        .in(Exam::getCourseId, teacherCourseIds))
                        .stream().map(Exam::getId).collect(Collectors.toList());
                if (teacherExamIds.isEmpty()) {
                    return ClassAnalysisVO.builder().scoreDistribution(Collections.emptyList()).averageScore(0.0)
                            .questionAccuracies(Collections.emptyList()).build();
                }
                recordQw.in(ExamRecord::getExamId, teacherExamIds);
            }
        } else {
            if (examId != null) {
                recordQw.eq(ExamRecord::getExamId, examId);
            } else if (courseId != null) {
                List<Long> courseExamIds = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                        .eq(Exam::getCourseId, courseId))
                        .stream().map(Exam::getId).collect(Collectors.toList());
                if (courseExamIds.isEmpty()) {
                    return ClassAnalysisVO.builder().scoreDistribution(Collections.emptyList()).averageScore(0.0)
                            .questionAccuracies(Collections.emptyList()).build();
                }
                recordQw.in(ExamRecord::getExamId, courseExamIds);
            }
        }
        List<ExamRecord> examRecords = examRecordMapper.selectList(recordQw);

        if (examRecords.isEmpty()) {
            return ClassAnalysisVO.builder()
                    .scoreDistribution(Collections.emptyList())
                    .averageScore(0.0)
                    .questionAccuracies(Collections.emptyList())
                    .build();
        }

        // 3. 统计分布
        String[] labels = { "90-100%", "80-89%", "70-79%", "60-69%", "0-59%" };
        int[] distribution = new int[5];
        double totalScoreSum = 0;
        Integer singleExamMaxScore = null;

        if (examId != null) {
            // 单场考试模式：统计学生分数分布
            Exam exam = examMapper.selectById(examId);
            if (exam != null) {
                Paper paper = paperMapper.selectById(exam.getPaperId());
                singleExamMaxScore = (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;
            }
            int maxScore = (singleExamMaxScore != null) ? singleExamMaxScore : 100;

            for (ExamRecord er : examRecords) {
                int score = (er.getTotalScore() != null ? er.getTotalScore() : 0);
                totalScoreSum += score;
                double ratio = (double) score / maxScore;
                if (ratio >= 0.9)
                    distribution[0]++;
                else if (ratio >= 0.8)
                    distribution[1]++;
                else if (ratio >= 0.7)
                    distribution[2]++;
                else if (ratio >= 0.6)
                    distribution[3]++;
                else
                    distribution[4]++;
            }
        } else {
            // 全部考试模式：统计题目得分率分布
            List<Long> recordIds = examRecords.stream().map(ExamRecord::getId).collect(Collectors.toList());
            List<AnswerDetail> details = answerDetailMapper.selectList(
                    new LambdaQueryWrapper<AnswerDetail>().in(AnswerDetail::getExamRecordId, recordIds));

            // 计算班级平均分（基于所有考试记录）
            totalScoreSum = examRecords.stream().mapToInt(er -> er.getTotalScore() != null ? er.getTotalScore() : 0)
                    .sum();

            // 按题目 ID 分组答题详情
            Map<Long, List<AnswerDetail>> qMap = details.stream()
                    .filter(d -> d.getQuestionId() != null)
                    .collect(Collectors.groupingBy(AnswerDetail::getQuestionId));

            // 获取所有题目的满分
            Set<Long> qIds = qMap.keySet();
            if (!qIds.isEmpty()) {
                // 找到这些题目所属的所有试卷
                List<Long> examIds = examRecords.stream().map(ExamRecord::getExamId).distinct()
                        .collect(Collectors.toList());
                List<Exam> exams = examMapper.selectBatchIds(examIds);
                List<Long> paperIds = exams.stream().map(Exam::getPaperId).collect(Collectors.toList());

                List<PaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                        .in(PaperQuestion::getPaperId, paperIds)
                        .in(PaperQuestion::getQuestionId, qIds));

                // 一个题目在不同试卷可能分值不同，这里取平均或者按答题记录对应的试卷找
                // 为了简化且符合大多数场景，我们按答题记录关联
                Map<Long, Map<Long, Integer>> paperQuestionScoreMap = new HashMap<>(); // paperId -> questionId -> score
                for (PaperQuestion pq : pqs) {
                    paperQuestionScoreMap.computeIfAbsent(pq.getPaperId(), k -> new HashMap<>()).put(pq.getQuestionId(),
                            pq.getScore());
                }

                // 重新统计得分率
                Map<Long, Long> examPaperMap = exams.stream().collect(Collectors.toMap(Exam::getId, Exam::getPaperId));
                Map<Long, Long> recordExamMap = examRecords.stream()
                        .collect(Collectors.toMap(ExamRecord::getId, ExamRecord::getExamId));

                for (Map.Entry<Long, List<AnswerDetail>> entry : qMap.entrySet()) {
                    Long qId = entry.getKey();
                    List<AnswerDetail> qDetails = entry.getValue();

                    double totalQScore = 0;
                    double totalQMaxScore = 0;

                    for (AnswerDetail d : qDetails) {
                        Long examIdOfRecord = recordExamMap.get(d.getExamRecordId());
                        if (examIdOfRecord == null)
                            continue;

                        Long paperId = examPaperMap.get(examIdOfRecord);
                        if (paperId == null)
                            continue;

                        Integer maxScore = paperQuestionScoreMap.getOrDefault(paperId, Collections.emptyMap())
                                .getOrDefault(qId, 5);

                        totalQScore += (d.getScore() != null ? d.getScore() : 0);
                        totalQMaxScore += maxScore;
                    }

                    double qAccuracy = totalQMaxScore == 0 ? 0 : totalQScore / totalQMaxScore;
                    if (qAccuracy >= 0.9)
                        distribution[0]++;
                    else if (qAccuracy >= 0.8)
                        distribution[1]++;
                    else if (qAccuracy >= 0.7)
                        distribution[2]++;
                    else if (qAccuracy >= 0.6)
                        distribution[3]++;
                    else
                        distribution[4]++;
                }
            }
        }

        List<ClassAnalysisVO.ScoreDistributionVO> distVOs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            distVOs.add(new ClassAnalysisVO.ScoreDistributionVO(labels[i], distribution[i]));
        }

        // 4. 题目正确率 (仅单场考试模式)
        List<ClassAnalysisVO.QuestionAccuracyVO> accuracyVOs = new ArrayList<>();
        if (examId != null) {
            List<Long> recordIds = examRecords.stream().map(ExamRecord::getId).collect(Collectors.toList());
            Exam exam = examMapper.selectById(examId);
            List<PaperQuestion> pqs = paperQuestionMapper.selectList(
                    new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, exam.getPaperId()));
            Set<Long> targetQuestionIds = pqs.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toSet());

            if (!recordIds.isEmpty()) {
                List<AnswerDetail> details = answerDetailMapper.selectList(
                        new LambdaQueryWrapper<AnswerDetail>().in(AnswerDetail::getExamRecordId, recordIds));
                Map<Long, List<Boolean>> qMap = new HashMap<>();

                for (AnswerDetail detail : details) {
                    if (!targetQuestionIds.contains(detail.getQuestionId()))
                        continue;
                    qMap.putIfAbsent(detail.getQuestionId(), new ArrayList<>());
                    qMap.get(detail.getQuestionId()).add(detail.getIsCorrect() != null && detail.getIsCorrect() == 1);
                }

                if (!qMap.isEmpty()) {
                    List<Question> questions = questionMapper.selectBatchIds(qMap.keySet());
                    Map<Long, String> questionContentMap = questions.stream()
                            .collect(Collectors.toMap(Question::getId, Question::getContent));
                    for (Map.Entry<Long, List<Boolean>> entry : qMap.entrySet()) {
                        String content = questionContentMap.getOrDefault(entry.getKey(), "未知题目");
                        List<Boolean> results = entry.getValue();
                        double accuracy = results.isEmpty() ? 0.0
                                : (double) results.stream().filter(r -> r).count() / results.size();
                        accuracyVOs.add(new ClassAnalysisVO.QuestionAccuracyVO(entry.getKey(), content, accuracy));
                    }
                    accuracyVOs.sort(Comparator.comparing(ClassAnalysisVO.QuestionAccuracyVO::getAccuracy));
                }
            }
        }

        return ClassAnalysisVO.builder()
                .scoreDistribution(distVOs)
                .averageScore(totalScoreSum / examRecords.size())
                .maxScore(singleExamMaxScore)
                .questionAccuracies(accuracyVOs)
                .build();
    }

    @Override
    public List<com.example.zaixiantiku.entity.Class> getTeacherClasses(Long courseId) {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isAdmin = loginUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        List<Long> targetCourseIds;
        if (isAdmin) {
            if (courseId != null) {
                targetCourseIds = Collections.singletonList(courseId);
            } else {
                return classMapper.selectList(null);
            }
        } else {
            Long teacherId = loginUser.getUser().getId();
            List<Long> managedCourseIds = courseTeacherMapper.selectList(new LambdaQueryWrapper<CourseTeacher>()
                    .eq(CourseTeacher::getTeacherId, teacherId))
                    .stream().map(CourseTeacher::getCourseId).collect(Collectors.toList());
            if (managedCourseIds.isEmpty())
                return Collections.emptyList();

            if (courseId != null) {
                if (managedCourseIds.contains(courseId)) {
                    targetCourseIds = Collections.singletonList(courseId);
                } else {
                    return Collections.emptyList();
                }
            } else {
                targetCourseIds = managedCourseIds;
            }
        }

        List<Long> studentIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                .in(CourseStudent::getCourseId, targetCourseIds))
                .stream().map(CourseStudent::getStudentId).collect(Collectors.toList());
        if (studentIds.isEmpty())
            return Collections.emptyList();

        List<Long> classIds = studentClassMapper.selectList(new LambdaQueryWrapper<StudentClass>()
                .in(StudentClass::getStudentId, studentIds))
                .stream().map(StudentClass::getClassId).distinct().collect(Collectors.toList());
        if (classIds.isEmpty())
            return Collections.emptyList();

        return classMapper.selectBatchIds(classIds);
    }

    @Override
    public List<Exam> getExams(Long classId, Long courseId) {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isAdmin = loginUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        LambdaQueryWrapper<Exam> examQw = new LambdaQueryWrapper<Exam>();

        if (courseId != null) {
            examQw.eq(Exam::getCourseId, courseId);
        }

        if (classId != null) {
            List<ExamClass> examClasses = examClassMapper.selectList(
                    new LambdaQueryWrapper<ExamClass>().eq(ExamClass::getClassId, classId));
            Set<Long> examIds = examClasses.stream().map(ExamClass::getExamId).collect(Collectors.toSet());

            // 补充有成绩记录的考试
            List<StudentClass> studentsInClass = studentClassMapper.selectList(
                    new LambdaQueryWrapper<StudentClass>().eq(StudentClass::getClassId, classId));
            if (!studentsInClass.isEmpty()) {
                List<Long> studentIds = studentsInClass.stream().map(StudentClass::getStudentId)
                        .collect(Collectors.toList());
                List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                        .in(ExamRecord::getUserId, studentIds)
                        .select(ExamRecord::getExamId));
                examIds.addAll(records.stream().map(ExamRecord::getExamId).collect(Collectors.toSet()));
            }

            if (examIds.isEmpty())
                return Collections.emptyList();
            examQw.in(Exam::getId, examIds);
        }

        if (!isAdmin) {
            List<Long> teacherCourseIds = courseTeacherMapper.selectList(new LambdaQueryWrapper<CourseTeacher>()
                    .eq(CourseTeacher::getTeacherId, loginUser.getUser().getId()))
                    .stream().map(CourseTeacher::getCourseId).collect(Collectors.toList());
            if (teacherCourseIds.isEmpty())
                return Collections.emptyList();
            examQw.in(Exam::getCourseId, teacherCourseIds);
        }

        examQw.orderByDesc(Exam::getStartTime);
        return examMapper.selectList(examQw);
    }

    @Override
    public List<com.example.zaixiantiku.entity.Class> getAllClasses(Long courseId) {
        if (courseId == null) {
            return classMapper.selectList(null);
        }
        return getTeacherClasses(courseId); // 管理员传 courseId 时复用 getTeacherClasses 的逻辑即可
    }

    @Override
    public GlobalAnalysisVO getGlobalAnalysis(Long courseId) {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isAdmin = loginUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        List<ExamRecord> allRecords;
        List<Exam> allExamsInDb;
        List<com.example.zaixiantiku.entity.Class> allClasses;

        if (isAdmin) {
            LambdaQueryWrapper<Exam> examQw = new LambdaQueryWrapper<>();
            if (courseId != null) {
                examQw.eq(Exam::getCourseId, courseId);
            }
            allExamsInDb = examMapper.selectList(examQw);

            if (courseId != null) {
                List<Long> examIds = allExamsInDb.stream().map(Exam::getId).collect(Collectors.toList());
                if (examIds.isEmpty()) {
                    allRecords = Collections.emptyList();
                } else {
                    allRecords = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                            .in(ExamRecord::getExamId, examIds)
                            .ge(ExamRecord::getStatus, 1));
                }
                allClasses = getTeacherClasses(courseId);
            } else {
                allRecords = examRecordMapper
                        .selectList(new LambdaQueryWrapper<ExamRecord>().ge(ExamRecord::getStatus, 1));
                allClasses = classMapper.selectList(null);
            }
        } else {
            Long teacherId = loginUser.getUser().getId();
            List<Long> teacherCourseIds = courseTeacherMapper.selectList(new LambdaQueryWrapper<CourseTeacher>()
                    .eq(CourseTeacher::getTeacherId, teacherId))
                    .stream().map(CourseTeacher::getCourseId).collect(Collectors.toList());

            if (teacherCourseIds.isEmpty()) {
                return GlobalAnalysisVO.builder()
                        .totalExams(0).totalStudents(0).averageScore(0.0).passRate(0.0)
                        .classPerformance(Collections.emptyList()).recentExams(Collections.emptyList())
                        .build();
            }

            // 如果指定了 courseId，校验权限
            List<Long> targetCourseIds;
            if (courseId != null) {
                if (!teacherCourseIds.contains(courseId)) {
                    throw new RuntimeException("您无权查看该课程的分析数据");
                }
                targetCourseIds = Collections.singletonList(courseId);
            } else {
                targetCourseIds = teacherCourseIds;
            }

            allExamsInDb = examMapper
                    .selectList(new LambdaQueryWrapper<Exam>().in(Exam::getCourseId, targetCourseIds));
            List<Long> examIds = allExamsInDb.stream().map(Exam::getId).collect(Collectors.toList());
            if (examIds.isEmpty()) {
                allRecords = Collections.emptyList();
            } else {
                allRecords = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                        .in(ExamRecord::getExamId, examIds)
                        .ge(ExamRecord::getStatus, 1));
            }
            allClasses = getTeacherClasses(courseId);
        }

        if (allExamsInDb.isEmpty()) {
            return GlobalAnalysisVO.builder()
                    .totalExams(0).totalStudents(0).averageScore(0.0).passRate(0.0)
                    .classPerformance(Collections.emptyList()).recentExams(Collections.emptyList())
                    .build();
        }

        int totalExams = allExamsInDb.size();
        Set<Long> studentIds = allRecords.stream().map(ExamRecord::getUserId).collect(Collectors.toSet());
        double avgScore = allRecords.isEmpty() ? 0.0
                : allRecords.stream().mapToInt(ExamRecord::getTotalScore).average().orElse(0.0);

        Map<Long, Integer> examMaxScoreMap = new HashMap<>();
        long passCount = allRecords.stream().filter(er -> {
            Integer maxScore = examMaxScoreMap.computeIfAbsent(er.getExamId(), id -> {
                Exam exam = examMapper.selectById(id);
                if (exam != null) {
                    Paper paper = paperMapper.selectById(exam.getPaperId());
                    return (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;
                }
                return 100;
            });
            return er.getTotalScore() >= maxScore * 0.6;
        }).count();
        double passRate = allRecords.isEmpty() ? 0.0 : (double) passCount / allRecords.size();

        List<GlobalAnalysisVO.ClassPerformanceVO> classPerf = new ArrayList<>();
        for (com.example.zaixiantiku.entity.Class c : allClasses) {
            List<StudentClass> studentsInClass = studentClassMapper.selectList(
                    new LambdaQueryWrapper<StudentClass>().eq(StudentClass::getClassId, c.getId()));
            List<Long> classStudentIds = studentsInClass.stream().map(StudentClass::getStudentId)
                    .collect(Collectors.toList());
            if (classStudentIds.isEmpty())
                continue;
            List<ExamRecord> classRecords = allRecords.stream()
                    .filter(er -> classStudentIds.contains(er.getUserId()))
                    .collect(Collectors.toList());
            if (classRecords.isEmpty())
                continue;
            double classAvg = classRecords.stream().mapToInt(ExamRecord::getTotalScore).average().orElse(0.0);
            int classMax = classRecords.stream().mapToInt(ExamRecord::getTotalScore).max().orElse(0);
            long classPassCount = classRecords.stream().filter(er -> {
                Integer maxScore = examMaxScoreMap.get(er.getExamId());
                return er.getTotalScore() >= (maxScore != null ? maxScore : 100) * 0.6;
            }).count();
            classPerf.add(GlobalAnalysisVO.ClassPerformanceVO.builder()
                    .classId(c.getId()).className(c.getClassName()).averageScore(classAvg).maxScore(classMax)
                    .passRate((double) classPassCount / classRecords.size())
                    .build());
        }

        List<GlobalAnalysisVO.ExamBriefVO> recentExams = allExamsInDb.stream()
                .sorted(Comparator.comparing(Exam::getStartTime).reversed())
                .limit(10)
                .map(exam -> {
                    List<ExamRecord> examRecords = allRecords.stream()
                            .filter(er -> er.getExamId().equals(exam.getId()))
                            .collect(Collectors.toList());
                    int participantCount = examRecords.size();
                    if (participantCount == 0) {
                        Paper paper = paperMapper.selectById(exam.getPaperId());
                        int maxScore = (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;
                        return GlobalAnalysisVO.ExamBriefVO.builder()
                                .id(exam.getId()).examName(exam.getExamName()).averageScore(0.0).maxScore(maxScore)
                                .passRate(0.0).participantCount(0).status("empty")
                                .build();
                    }
                    double examAvg = examRecords.stream().mapToInt(ExamRecord::getTotalScore).average().orElse(0.0);
                    Paper paper = paperMapper.selectById(exam.getPaperId());
                    int maxScore = (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;
                    long examPassCount = examRecords.stream().filter(er -> er.getTotalScore() >= maxScore * 0.6)
                            .count();
                    return GlobalAnalysisVO.ExamBriefVO.builder()
                            .id(exam.getId()).examName(exam.getExamName()).averageScore(examAvg).maxScore(maxScore)
                            .passRate((double) examPassCount / participantCount).participantCount(participantCount)
                            .status("normal")
                            .build();
                }).collect(Collectors.toList());

        return GlobalAnalysisVO.builder()
                .totalExams(totalExams).totalStudents(studentIds.size()).averageScore(avgScore).passRate(passRate)
                .classPerformance(classPerf).recentExams(recentExams)
                .build();
    }

    @Override
    public GlobalAnalysisVO getAdminDashboardStats() {
        // 1. 用户总数 (status=1)
        Integer totalUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getStatus, 1)).intValue();

        // 2. 今日活跃 (今日内有登录行为的用户数)
        java.time.LocalDateTime todayStart = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
                .withNano(0);
        List<Log> todayLogs = logMapper.selectList(new LambdaQueryWrapper<Log>()
                .ge(Log::getCreateTime, todayStart)
                .eq(Log::getOperation, "用户登录"));
        Integer activeToday = (int) todayLogs.stream().map(Log::getUserId).distinct().count();

        // 3. 试题总量 (status=2 已发布)
        Integer totalQuestions = questionMapper
                .selectCount(new LambdaQueryWrapper<Question>().eq(Question::getStatus, 2)).intValue();

        // 4. 考试场次 (所有未删除的考试)
        Integer totalExams = examMapper.selectCount(null).intValue();

        return GlobalAnalysisVO.builder()
                .totalUsers(totalUsers)
                .activeToday(activeToday)
                .totalQuestions(totalQuestions)
                .totalExams(totalExams)
                .build();
    }

    @Override
    public List<Map<String, Object>> getStudentEnrolledCourses() {
        Long userId = getUserId();
        List<CourseStudent> enrollments = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getStudentId, userId));

        if (enrollments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> courseIds = enrollments.stream().map(CourseStudent::getCourseId).collect(Collectors.toList());
        List<Course> courses = courseMapper.selectBatchIds(courseIds);

        return courses.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getCourseName());
            return map;
        }).collect(Collectors.toList());
    }

    private Long getUserId() {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return loginUser.getUser().getId();
    }

    @Override
    public StudentAnalysisVO getStudentExamSummary(Long courseId) {
        if (courseId == null) {
            return StudentAnalysisVO.builder()
                    .totalExamCount(0).avgExamScoreRate(0.0).maxExamScore(0).maxExamTotalScore(0)
                    .build();
        }

        Long userId = getUserId();
        // 权限校验：学生必须选修了该课程
        Long enrollCount = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getStudentId, userId)
                .eq(CourseStudent::getCourseId, courseId));
        if (enrollCount == null || enrollCount == 0) {
            throw new RuntimeException("您未选修该课程，无权查看考试概况");
        }

        // 获取该课程下的所有考试ID
        List<Long> examIdsInCourse = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                .eq(Exam::getCourseId, courseId))
                .stream().map(Exam::getId).collect(Collectors.toList());

        if (examIdsInCourse.isEmpty()) {
            return StudentAnalysisVO.builder()
                    .totalExamCount(0).avgExamScoreRate(0.0).maxExamScore(0).maxExamTotalScore(0)
                    .build();
        }

        LambdaQueryWrapper<ExamRecord> examRecordQw = new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .ge(ExamRecord::getStatus, 1) // 状态 >= 1 表示已提交或已批阅
                .in(ExamRecord::getExamId, examIdsInCourse);

        List<ExamRecord> courseExamRecords = examRecordMapper.selectList(examRecordQw);

        int totalExamCount = courseExamRecords.size();
        int maxExamScore = 0;
        int maxExamTotalScore = 0;
        double totalExamScoreSum = 0;
        double totalMaxScoreSum = 0;

        // 批量获取所有相关考试和试卷信息，避免循环查询
        Map<Long, Exam> examMap = new HashMap<>();
        Map<Long, Paper> paperMap = new HashMap<>();
        if (!courseExamRecords.isEmpty()) {
            Set<Long> recordExamIds = courseExamRecords.stream().map(ExamRecord::getExamId).collect(Collectors.toSet());
            examMap = examMapper.selectBatchIds(recordExamIds).stream()
                    .collect(Collectors.toMap(Exam::getId, e -> e));
            Set<Long> paperIds = examMap.values().stream().map(Exam::getPaperId).collect(Collectors.toSet());
            if (!paperIds.isEmpty()) {
                paperMap = paperMapper.selectBatchIds(paperIds).stream()
                        .collect(Collectors.toMap(Paper::getId, p -> p));
            }
        }

        for (ExamRecord er : courseExamRecords) {
            int score = (er.getTotalScore() != null) ? er.getTotalScore() : 0;
            totalExamScoreSum += score;

            Exam exam = examMap.get(er.getExamId());
            int maxScore = 100;
            if (exam != null) {
                Paper paper = paperMap.get(exam.getPaperId());
                maxScore = (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;
                totalMaxScoreSum += maxScore;

                if (score > maxExamScore || (maxExamScore == 0 && maxExamTotalScore == 0)) {
                    maxExamScore = score;
                    maxExamTotalScore = maxScore;
                }
            }
        }

        double avgExamScoreRate = totalMaxScoreSum == 0 ? 0.0 : totalExamScoreSum / totalMaxScoreSum;

        return StudentAnalysisVO.builder()
                .totalExamCount(totalExamCount)
                .avgExamScoreRate(avgExamScoreRate)
                .maxExamScore(maxExamScore)
                .maxExamTotalScore(maxExamTotalScore)
                .build();
    }

    @Override
    public PageResult<StudentExamRecordVO> getStudentExamRecords(Integer page, Integer size, Long courseId) {
        Long userId = getUserId();

        LambdaQueryWrapper<ExamRecord> examRecordQw = new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .ge(ExamRecord::getStatus, 1) // 状态 >= 1 表示已提交或已批阅
                .orderByDesc(ExamRecord::getSubmitTime);

        if (courseId != null) {
            // 权限校验：学生必须选修了该课程
            Long enrollCount = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getStudentId, userId)
                    .eq(CourseStudent::getCourseId, courseId));
            if (enrollCount == null || enrollCount == 0) {
                throw new RuntimeException("您未选修该课程，无权查看考试记录");
            }

            List<Long> examIdsInCourse = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                    .eq(Exam::getCourseId, courseId))
                    .stream().map(Exam::getId).collect(Collectors.toList());

            if (examIdsInCourse.isEmpty()) {
                return PageResult.of(0L, new ArrayList<>());
            }
            examRecordQw.in(ExamRecord::getExamId, examIdsInCourse);
        }

        PageHelper.startPage(page, size);
        List<ExamRecord> records = examRecordMapper.selectList(examRecordQw);
        PageInfo<ExamRecord> pageInfo = new PageInfo<>(records);

        if (records.isEmpty()) {
            return PageResult.of(0L, new ArrayList<>());
        }

        // 批量获取所有相关考试和试卷信息，避免循环查询
        final Map<Long, Exam> examMap;
        final Map<Long, Paper> paperMap;
        if (!records.isEmpty()) {
            Set<Long> examIds = records.stream().map(ExamRecord::getExamId).collect(Collectors.toSet());
            examMap = examMapper.selectBatchIds(examIds).stream()
                    .collect(Collectors.toMap(Exam::getId, e -> e));
            Set<Long> paperIds = examMap.values().stream().map(Exam::getPaperId).collect(Collectors.toSet());
            if (!paperIds.isEmpty()) {
                paperMap = paperMapper.selectBatchIds(paperIds).stream()
                        .collect(Collectors.toMap(Paper::getId, p -> p));
            } else {
                paperMap = new HashMap<>();
            }
        } else {
            examMap = new HashMap<>();
            paperMap = new HashMap<>();
        }

        List<StudentExamRecordVO> voList = records.stream().map(er -> {
            Exam exam = examMap.get(er.getExamId());
            Paper paper = (exam != null) ? paperMap.get(exam.getPaperId()) : null;
            int totalScore = (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;
            double scoreRate = (double) er.getTotalScore() / totalScore;

            return StudentExamRecordVO.builder()
                    .id(er.getId())
                    .examId(er.getExamId())
                    .examName((exam != null) ? exam.getExamName() : "未知考试")
                    .submitTime(er.getSubmitTime())
                    .totalScore(er.getTotalScore())
                    .maxScore(totalScore)
                    .scoreRate(scoreRate)
                    .status(er.getStatus())
                    .build();
        }).collect(Collectors.toList());

        return PageResult.of(pageInfo.getTotal(), voList);
    }

    @Override
    public StudentExamRecordDetailVO getStudentExamDetail(Long recordId) {
        Long userId = getUserId();
        ExamRecord examRecord = examRecordMapper.selectById(recordId);
        if (examRecord == null || !examRecord.getUserId().equals(userId)) {
            throw new RuntimeException("考试记录不存在或无权查看");
        }
        if (examRecord.getStatus() < 1) {
            throw new RuntimeException("考试未完成或未提交，无法查看详情");
        }

        Exam exam = examMapper.selectById(examRecord.getExamId());
        if (exam == null) {
            throw new RuntimeException("考试信息不存在");
        }
        Paper paper = paperMapper.selectById(exam.getPaperId());
        if (paper == null) {
            throw new RuntimeException("试卷信息不存在");
        }

        // 1. 考试基本信息
        StudentExamRecordDetailVO detailVO = StudentExamRecordDetailVO.builder()
                .recordId(examRecord.getId())
                .examId(exam.getId())
                .examName(exam.getExamName())
                .courseId(exam.getCourseId())
                .courseName(courseMapper.selectById(exam.getCourseId()).getCourseName())
                .submitTime(examRecord.getSubmitTime())
                .totalScore(examRecord.getTotalScore())
                .maxScore(paper.getTotalScore())
                .scoreRate((double) examRecord.getTotalScore() / paper.getTotalScore())
                .status(examRecord.getStatus())
                .build();

        // 2. 题目详情
        List<AnswerDetail> answerDetails = answerDetailMapper.selectList(
                new LambdaQueryWrapper<AnswerDetail>().eq(AnswerDetail::getExamRecordId, recordId));

        final Map<Long, Question> questionMap;
        final Map<Long, PaperQuestion> paperQuestionMap;

        if (!answerDetails.isEmpty()) {
            Set<Long> questionIds = answerDetails.stream().map(AnswerDetail::getQuestionId).collect(Collectors.toSet());
            questionMap = questionMapper.selectBatchIds(questionIds).stream()
                    .collect(Collectors.toMap(Question::getId, q -> q));
            paperQuestionMap = paperQuestionMapper.selectList(
                    new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, paper.getId())
                            .in(PaperQuestion::getQuestionId, questionIds))
                    .stream().collect(Collectors.toMap(PaperQuestion::getQuestionId, pq -> pq));
        } else {
            questionMap = new HashMap<>();
            paperQuestionMap = new HashMap<>();
        }

        List<StudentExamRecordDetailVO.QuestionDetail> qDetails = answerDetails.stream().map(ad -> {
            Question q = questionMap.get(ad.getQuestionId());
            PaperQuestion pq = paperQuestionMap.get(ad.getQuestionId());
            if (q == null || pq == null)
                return null;

            return StudentExamRecordDetailVO.QuestionDetail.builder()
                    .questionId(q.getId())
                    .questionContent(q.getContent())
                    .questionType(questionTypeMapper.selectById(q.getTypeId()).getTypeName())
                    .options(parseOptions(q.getOptions()))
                    .correctAnswer(q.getAnswer())
                    .analysis(q.getAnalysis())
                    .score(pq.getScore())
                    .userAnswer(ad.getUserAnswer())
                    .userScore(ad.getScore())
                    .isCorrect(ad.getIsCorrect() == 1)
                    .build();
        }).filter(Objects::nonNull).collect(Collectors.toList());
        detailVO.setQuestionDetails(qDetails);

        // 3. 知识点掌握情况雷达图 (基于本次考试题目)
        Map<Long, List<Boolean>> knowledgePointResults = new HashMap<>();
        for (StudentExamRecordDetailVO.QuestionDetail qd : qDetails) {
            List<Long> kpIds = questionKnowledgeMapper.selectList(new LambdaQueryWrapper<QuestionKnowledge>()
                    .eq(QuestionKnowledge::getQuestionId, qd.getQuestionId()))
                    .stream().map(QuestionKnowledge::getKnowledgePointId).collect(Collectors.toList());
            for (Long kpId : kpIds) {
                knowledgePointResults.putIfAbsent(kpId, new ArrayList<>());
                knowledgePointResults.get(kpId).add(qd.getIsCorrect());
            }
        }

        List<StudentAnalysisVO.KnowledgeRadarVO> knowledgeRadar = new ArrayList<>();
        if (!knowledgePointResults.isEmpty()) {
            Map<Long, String> knowledgePointNameMap = knowledgePointMapper
                    .selectBatchIds(knowledgePointResults.keySet()).stream()
                    .collect(Collectors.toMap(KnowledgePoint::getId, KnowledgePoint::getName));
            for (Map.Entry<Long, List<Boolean>> entry : knowledgePointResults.entrySet()) {
                Long kpId = entry.getKey();
                List<Boolean> results = entry.getValue();
                long correctCount = results.stream().filter(Boolean::booleanValue).count();
                double accuracy = results.isEmpty() ? 0.0 : (double) correctCount / results.size();
                knowledgeRadar.add(new StudentAnalysisVO.KnowledgeRadarVO(
                        knowledgePointNameMap.getOrDefault(kpId, "未知"), accuracy));
            }
            knowledgeRadar.sort(Comparator.comparingDouble(StudentAnalysisVO.KnowledgeRadarVO::getValue));
        }
        detailVO.setKnowledgeRadar(knowledgeRadar);

        return detailVO;
    }

    private List<String> parseOptions(String json) {
        if (!StringUtils.hasText(json))
            return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<StudentAnalysisVO.TrendVO> getStudentExamScoreTrend(Long courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }

        Long userId = getUserId();
        // 权限校验：学生必须选修了该课程
        Long enrollCount = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getStudentId, userId)
                .eq(CourseStudent::getCourseId, courseId));
        if (enrollCount == null || enrollCount == 0) {
            throw new RuntimeException("您未选修该课程，无权查看考试成绩趋势");
        }

        // 获取该课程下的所有考试ID
        List<Long> examIdsInCourse = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                .eq(Exam::getCourseId, courseId))
                .stream().map(Exam::getId).collect(Collectors.toList());

        if (examIdsInCourse.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<ExamRecord> examRecordQw = new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .ge(ExamRecord::getStatus, 1) // 状态 >= 1 表示已提交或已批阅
                .in(ExamRecord::getExamId, examIdsInCourse)
                .orderByAsc(ExamRecord::getSubmitTime); // 按提交时间升序排列，用于趋势图

        List<ExamRecord> courseExamRecords = examRecordMapper.selectList(examRecordQw);

        if (courseExamRecords.isEmpty()) {
            return Collections.emptyList();
        }

        // 知识点趋势
        final Map<Long, Exam> examMap;
        final Map<Long, Paper> paperMap;
        if (!courseExamRecords.isEmpty()) {
            Set<Long> recordExamIds = courseExamRecords.stream().map(ExamRecord::getExamId).collect(Collectors.toSet());
            examMap = examMapper.selectBatchIds(recordExamIds).stream()
                    .collect(Collectors.toMap(Exam::getId, e -> e));
            Set<Long> paperIds = examMap.values().stream().map(Exam::getPaperId).collect(Collectors.toSet());
            if (!paperIds.isEmpty()) {
                paperMap = paperMapper.selectBatchIds(paperIds).stream()
                        .collect(Collectors.toMap(Paper::getId, p -> p));
            } else {
                paperMap = new HashMap<>();
            }
        } else {
            examMap = new HashMap<>();
            paperMap = new HashMap<>();
        }

        List<StudentAnalysisVO.TrendVO> trendList = courseExamRecords.stream().map(er -> {
            Exam exam = examMap.get(er.getExamId());
            Paper paper = (exam != null) ? paperMap.get(exam.getPaperId()) : null;
            int totalScore = (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;
            double scoreRate = (double) er.getTotalScore() / totalScore;
            String examName = (exam != null) ? exam.getExamName() : "未知考试";
            return new StudentAnalysisVO.TrendVO(examName, scoreRate);
        }).collect(Collectors.toList());

        return trendList;
    }
}
