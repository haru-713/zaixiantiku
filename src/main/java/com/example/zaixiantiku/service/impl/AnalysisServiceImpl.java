package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.zaixiantiku.entity.*;
import com.example.zaixiantiku.mapper.*;
import com.example.zaixiantiku.pojo.vo.ClassAnalysisVO;
import com.example.zaixiantiku.pojo.vo.GlobalAnalysisVO;
import com.example.zaixiantiku.pojo.vo.StudentAnalysisVO;
import com.example.zaixiantiku.security.LoginUser;
import com.example.zaixiantiku.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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
        Long userId = getUserId();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime startTime = null;

        if ("week".equals(timeRange)) {
            startTime = now.minusDays(7);
        } else if ("month".equals(timeRange)) {
            startTime = now.minusDays(30);
        }

        // --- 1. 获取练习数据 ---
        LambdaQueryWrapper<PracticeRecord> practiceQw = new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .isNotNull(PracticeRecord::getSubmitTime);
        if (courseId != null)
            practiceQw.eq(PracticeRecord::getCourseId, courseId);
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

        // --- 2. 获取考试数据 ---
        LambdaQueryWrapper<ExamRecord> examQw = new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .ge(ExamRecord::getStatus, 1);
        if (startTime != null)
            examQw.ge(ExamRecord::getSubmitTime, startTime);

        List<ExamRecord> examRecords = examRecordMapper.selectList(examQw);
        if (courseId != null) {
            examRecords = examRecords.stream().filter(er -> {
                Exam exam = examMapper.selectById(er.getExamId());
                return exam != null && courseId.equals(exam.getCourseId());
            }).collect(Collectors.toList());
        }

        int totalExamCount = examRecords.size();
        int maxExamScore = 0;
        int maxExamTotalScore = 0;
        double totalExamScoreSum = 0;
        double totalMaxScoreSum = 0;
        int[] scoreDist = new int[5]; // 0-59, 60-69, 70-79, 80-89, 90-100
        List<StudentAnalysisVO.RankingTrendVO> rankingTrends = new ArrayList<>();
        List<StudentAnalysisVO.RecentExamVO> recentExams = new ArrayList<>();
        Map<Long, Integer> examWrongCountMap = new HashMap<>();

        for (ExamRecord er : examRecords) {
            int score = er.getTotalScore() != null ? er.getTotalScore() : 0;
            totalExamScoreSum += score;

            // 获取考试满分以计算得分率
            Exam exam = examMapper.selectById(er.getExamId());
            int maxScore = 100;
            if (exam != null) {
                Paper paper = paperMapper.selectById(exam.getPaperId());
                maxScore = (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;
                totalMaxScoreSum += maxScore;

                // 记录近期考试详情
                recentExams.add(new StudentAnalysisVO.RecentExamVO(
                        exam.getExamName(),
                        score,
                        maxScore,
                        (double) score / maxScore,
                        er.getSubmitTime().format(formatter)));
            }

            if (score > maxExamScore || (maxExamScore == 0 && maxExamTotalScore == 0)) {
                maxExamScore = score;
                maxExamTotalScore = maxScore;
            }

            // 分数分布
            if (score < 60)
                scoreDist[0]++;
            else if (score < 70)
                scoreDist[1]++;
            else if (score < 80)
                scoreDist[2]++;
            else if (score < 90)
                scoreDist[3]++;
            else
                scoreDist[4]++;

            // 排名趋势
            if (exam != null) {
                int rank = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getExamId, er.getExamId())
                        .ge(ExamRecord::getStatus, 1)
                        .gt(ExamRecord::getTotalScore, score)).intValue() + 1;
                int totalStudents = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getExamId, er.getExamId())
                        .ge(ExamRecord::getStatus, 1)).intValue();
                rankingTrends.add(new StudentAnalysisVO.RankingTrendVO(exam.getExamName(), rank, totalStudents));
            }

            // 题目详情
            List<AnswerDetail> details = answerDetailMapper.selectList(
                    new LambdaQueryWrapper<AnswerDetail>().eq(AnswerDetail::getExamRecordId, er.getId()));
            for (AnswerDetail detail : details) {
                if (detail.getIsCorrect() == 0) {
                    examWrongCountMap.put(detail.getQuestionId(),
                            examWrongCountMap.getOrDefault(detail.getQuestionId(), 0) + 1);
                }
            }
        }

        // --- 3. 统计知识点雷达图 (基于练习数据) ---
        List<StudentAnalysisVO.KnowledgeMasteryVO> knowledgeRadar = new ArrayList<>();
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
                            .add(new StudentAnalysisVO.KnowledgeMasteryVO(kpNames.get(kpId), acc, results.size()));
                });
            }
        }

        // --- 4. 统计题型分析 (练习) ---
        List<StudentAnalysisVO.TypeStatVO> practiceTypeStats = new ArrayList<>();
        if (!practiceQIdResults.isEmpty()) {
            List<Question> questions = questionMapper.selectBatchIds(practiceQIdResults.keySet());
            Map<Integer, List<Boolean>> typeResults = new HashMap<>();
            for (Question q : questions) {
                List<Boolean> results = practiceQIdResults.get(q.getId());
                if (results != null) {
                    typeResults.putIfAbsent(q.getTypeId(), new ArrayList<>());
                    typeResults.get(q.getTypeId()).addAll(results);
                }
            }
            if (!typeResults.isEmpty()) {
                Map<Integer, String> typeNames = questionTypeMapper.selectBatchIds(typeResults.keySet()).stream()
                        .collect(Collectors.toMap(QuestionType::getId, QuestionType::getTypeName));
                typeResults.forEach((typeId, results) -> {
                    double acc = (double) results.stream().filter(b -> b).count() / results.size();
                    practiceTypeStats
                            .add(new StudentAnalysisVO.TypeStatVO(typeNames.get(typeId), results.size(), acc, null));
                });
            }
        }

        // --- 5. 错题 Top5 ---
        LambdaQueryWrapper<MistakeBook> mistakeQw = new LambdaQueryWrapper<MistakeBook>()
                .eq(MistakeBook::getUserId, userId);
        if (courseId != null)
            mistakeQw.eq(MistakeBook::getCourseId, courseId);
        List<MistakeBook> mistakes = mistakeBookMapper.selectList(mistakeQw);
        Map<Long, Integer> kpMistakeCount = new HashMap<>();
        for (MistakeBook m : mistakes) {
            List<QuestionKnowledge> qks = questionKnowledgeMapper.selectList(
                    new LambdaQueryWrapper<QuestionKnowledge>().eq(QuestionKnowledge::getQuestionId,
                            m.getQuestionId()));
            for (QuestionKnowledge qk : qks) {
                kpMistakeCount.put(qk.getKnowledgePointId(),
                        kpMistakeCount.getOrDefault(qk.getKnowledgePointId(), 0) + 1);
            }
        }
        List<StudentAnalysisVO.MistakePointVO> topMistakePoints = new ArrayList<>();
        if (!kpMistakeCount.isEmpty()) {
            Map<Long, String> kpNames = knowledgePointMapper.selectBatchIds(kpMistakeCount.keySet()).stream()
                    .collect(Collectors.toMap(KnowledgePoint::getId, KnowledgePoint::getName));
            topMistakePoints = kpMistakeCount.entrySet().stream()
                    .map(e -> new StudentAnalysisVO.MistakePointVO(kpNames.get(e.getKey()), e.getValue()))
                    .sorted(Comparator.comparing(StudentAnalysisVO.MistakePointVO::getCount).reversed())
                    .limit(5)
                    .collect(Collectors.toList());
        }

        // --- 6. 组装最终结果 ---
        return StudentAnalysisVO.builder()
                .totalPracticeCount(practiceRecords.size())
                .totalPracticeQuestions(totalPracticeQuestions)
                .avgPracticeAccuracy(
                        totalPracticeQuestions == 0 ? 0.0 : (double) totalPracticeCorrect / totalPracticeQuestions)
                .totalExamCount(totalExamCount)
                .avgExamScore(totalExamCount == 0 ? 0.0 : totalExamScoreSum / totalExamCount)
                .maxExamScore(maxExamScore)
                .maxExamTotalScore(maxExamTotalScore)
                .avgExamScoreRate(totalMaxScoreSum == 0 ? 0.0 : totalExamScoreSum / totalMaxScoreSum)
                .mistakeCount(mistakes.size())
                .recentExams(recentExams.stream()
                        .sorted(Comparator.comparing(StudentAnalysisVO.RecentExamVO::getSubmitTime).reversed())
                        .limit(10)
                        .collect(Collectors.toList()))
                .practiceTrend(practiceTrendMap.entrySet().stream()
                        .map(e -> {
                            double acc = (double) e.getValue().stream().filter(b -> b).count() / e.getValue().size();
                            return new StudentAnalysisVO.TrendVO(e.getKey(), acc);
                        }).collect(Collectors.toList()))
                .knowledgeRadar(knowledgeRadar)
                .practiceTypeStats(practiceTypeStats)
                .topMistakePoints(topMistakePoints)
                .examScoreDist(Arrays.asList(
                        new StudentAnalysisVO.ScoreDistVO("0-59", scoreDist[0]),
                        new StudentAnalysisVO.ScoreDistVO("60-69", scoreDist[1]),
                        new StudentAnalysisVO.ScoreDistVO("70-79", scoreDist[2]),
                        new StudentAnalysisVO.ScoreDistVO("80-89", scoreDist[3]),
                        new StudentAnalysisVO.ScoreDistVO("90-100", scoreDist[4])))
                .rankingTrend(rankingTrends)
                .examTypeStats(new ArrayList<>())
                .highFreqMistakes(examWrongCountMap.entrySet().stream()
                        .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                        .limit(5)
                        .map(e -> {
                            Question q = questionMapper.selectById(e.getKey());
                            return new StudentAnalysisVO.HighFreqMistakeVO(e.getKey(),
                                    q != null ? q.getContent() : "未知题目", e.getValue());
                        }).collect(Collectors.toList()))
                .build();
    }

    @Override
    public ClassAnalysisVO getClassAnalysis(Long classId, Long examId) {
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

            if (examId != null) {
                Exam exam = examMapper.selectById(examId);
                if (exam == null || !teacherCourseIds.contains(exam.getCourseId())) {
                    return ClassAnalysisVO.builder().scoreDistribution(Collections.emptyList()).averageScore(0.0)
                            .questionAccuracies(Collections.emptyList()).build();
                }
                recordQw.eq(ExamRecord::getExamId, examId);
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
        int[] distribution = new int[5];
        double totalScoreSum = 0;
        Map<Long, Integer> examMaxScoreMap = new HashMap<>();
        Integer singleExamMaxScore = null;
        if (examId != null) {
            Exam exam = examMapper.selectById(examId);
            if (exam != null) {
                Paper paper = paperMapper.selectById(exam.getPaperId());
                singleExamMaxScore = (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;
            }
        }

        for (ExamRecord er : examRecords) {
            totalScoreSum += (er.getTotalScore() != null ? er.getTotalScore() : 0);
            Integer maxScore = examMaxScoreMap.computeIfAbsent(er.getExamId(), id -> {
                Exam exam = examMapper.selectById(id);
                if (exam != null) {
                    Paper paper = paperMapper.selectById(exam.getPaperId());
                    return (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;
                }
                return 100;
            });
            double ratio = (double) (er.getTotalScore() != null ? er.getTotalScore() : 0) / maxScore;
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

        String[] labels = { "90-100%", "80-89%", "70-79%", "60-69%", "0-59%" };
        List<ClassAnalysisVO.ScoreDistributionVO> distVOs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            distVOs.add(new ClassAnalysisVO.ScoreDistributionVO(labels[i], distribution[i]));
        }

        // 4. 题目正确率
        List<ClassAnalysisVO.QuestionAccuracyVO> accuracyVOs = new ArrayList<>();
        if (examId != null) {
            List<Long> recordIds = examRecords.stream().map(ExamRecord::getId).collect(Collectors.toList());
            Set<Long> targetQuestionIds = new HashSet<>();
            Exam exam = examMapper.selectById(examId);
            if (exam != null) {
                List<PaperQuestion> pqs = paperQuestionMapper.selectList(
                        new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, exam.getPaperId()));
                targetQuestionIds = pqs.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toSet());
            }

            if (!recordIds.isEmpty()) {
                List<AnswerDetail> details = answerDetailMapper.selectList(
                        new LambdaQueryWrapper<AnswerDetail>().in(AnswerDetail::getExamRecordId, recordIds));
                Map<Long, List<Boolean>> qMap = new HashMap<>();
                Map<Long, Integer> recordStatusMap = examRecords.stream()
                        .collect(Collectors.toMap(ExamRecord::getId, ExamRecord::getStatus));

                for (AnswerDetail detail : details) {
                    if (!targetQuestionIds.isEmpty() && !targetQuestionIds.contains(detail.getQuestionId()))
                        continue;
                    Integer status = recordStatusMap.get(detail.getExamRecordId());
                    if (status != null && status == 2) {
                        qMap.putIfAbsent(detail.getQuestionId(), new ArrayList<>());
                        qMap.get(detail.getQuestionId()).add(detail.getIsCorrect() == 1);
                    }
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
    public List<com.example.zaixiantiku.entity.Class> getTeacherClasses() {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isAdmin = loginUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin)
            return classMapper.selectList(null);
        Long teacherId = loginUser.getUser().getId();
        List<Long> courseIds = courseTeacherMapper.selectList(new LambdaQueryWrapper<CourseTeacher>()
                .eq(CourseTeacher::getTeacherId, teacherId))
                .stream().map(CourseTeacher::getCourseId).collect(Collectors.toList());
        if (courseIds.isEmpty())
            return Collections.emptyList();
        List<Long> studentIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                .in(CourseStudent::getCourseId, courseIds))
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
    public List<Exam> getClassExams(Long classId) {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isAdmin = loginUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        List<StudentClass> studentsInClass = studentClassMapper.selectList(
                new LambdaQueryWrapper<StudentClass>().eq(StudentClass::getClassId, classId));
        if (studentsInClass.isEmpty())
            return Collections.emptyList();
        List<Long> studentIds = studentsInClass.stream().map(StudentClass::getStudentId).collect(Collectors.toList());
        Set<Long> examIds = new HashSet<>();
        List<ExamClass> examClasses = examClassMapper.selectList(
                new LambdaQueryWrapper<ExamClass>().eq(ExamClass::getClassId, classId));
        examIds.addAll(examClasses.stream().map(ExamClass::getExamId).collect(Collectors.toSet()));
        List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .in(ExamRecord::getUserId, studentIds)
                .select(ExamRecord::getExamId));
        examIds.addAll(records.stream().map(ExamRecord::getExamId).collect(Collectors.toSet()));
        if (examIds.isEmpty())
            return Collections.emptyList();
        LambdaQueryWrapper<Exam> examQw = new LambdaQueryWrapper<Exam>()
                .in(Exam::getId, examIds)
                .orderByDesc(Exam::getStartTime);
        if (!isAdmin) {
            List<Long> teacherCourseIds = courseTeacherMapper.selectList(new LambdaQueryWrapper<CourseTeacher>()
                    .eq(CourseTeacher::getTeacherId, loginUser.getUser().getId()))
                    .stream().map(CourseTeacher::getCourseId).collect(Collectors.toList());
            if (teacherCourseIds.isEmpty())
                return Collections.emptyList();
            examQw.in(Exam::getCourseId, teacherCourseIds);
        }
        return examMapper.selectList(examQw);
    }

    @Override
    public List<com.example.zaixiantiku.entity.Class> getAllClasses() {
        return classMapper.selectList(null);
    }

    @Override
    public GlobalAnalysisVO getGlobalAnalysis() {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isAdmin = loginUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        List<ExamRecord> allRecords;
        List<Exam> allExamsInDb;
        List<com.example.zaixiantiku.entity.Class> allClasses;

        if (isAdmin) {
            allRecords = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>().ge(ExamRecord::getStatus, 1));
            allExamsInDb = examMapper.selectList(null);
            allClasses = classMapper.selectList(null);
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

            allExamsInDb = examMapper
                    .selectList(new LambdaQueryWrapper<Exam>().in(Exam::getCourseId, teacherCourseIds));
            List<Long> examIds = allExamsInDb.stream().map(Exam::getId).collect(Collectors.toList());
            if (examIds.isEmpty()) {
                allRecords = Collections.emptyList();
            } else {
                allRecords = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                        .in(ExamRecord::getExamId, examIds)
                        .ge(ExamRecord::getStatus, 1));
            }
            allClasses = getTeacherClasses();
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
                        return GlobalAnalysisVO.ExamBriefVO.builder()
                                .id(exam.getId()).examName(exam.getExamName()).averageScore(0.0).maxScore(0)
                                .passRate(0.0).participantCount(0).status("empty")
                                .build();
                    }
                    double examAvg = examRecords.stream().mapToInt(ExamRecord::getTotalScore).average().orElse(0.0);
                    int examMax = examRecords.stream().mapToInt(ExamRecord::getTotalScore).max().orElse(0);
                    Paper paper = paperMapper.selectById(exam.getPaperId());
                    int maxScore = (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;
                    long examPassCount = examRecords.stream().filter(er -> er.getTotalScore() >= maxScore * 0.6)
                            .count();
                    return GlobalAnalysisVO.ExamBriefVO.builder()
                            .id(exam.getId()).examName(exam.getExamName()).averageScore(examAvg).maxScore(examMax)
                            .passRate((double) examPassCount / participantCount).participantCount(participantCount)
                            .status("normal")
                            .build();
                }).collect(Collectors.toList());

        return GlobalAnalysisVO.builder()
                .totalExams(totalExams).totalStudents(studentIds.size()).averageScore(avgScore).passRate(passRate)
                .classPerformance(classPerf).recentExams(recentExams)
                .build();
    }

    private Long getUserId() {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return loginUser.getUser().getId();
    }
}
