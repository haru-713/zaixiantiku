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
        double totalExamScoreSum = 0;
        double totalMaxScoreSum = 0;
        int[] scoreDist = new int[5]; // 0-59, 60-69, 70-79, 80-89, 90-100
        List<StudentAnalysisVO.RankingTrendVO> rankingTrends = new ArrayList<>();
        List<StudentAnalysisVO.RecentExamVO> recentExams = new ArrayList<>();
        Map<Long, Integer> examWrongCountMap = new HashMap<>();

        for (ExamRecord er : examRecords) {
            int score = er.getTotalScore() != null ? er.getTotalScore() : 0;
            totalExamScoreSum += score;
            if (score > maxExamScore)
                maxExamScore = score;

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

            // 分数分布 (这里虽然前端移除了，但后端逻辑可以保留作为统计基础)
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
                // 这里可以进一步统计考试中的题型得分率
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
                .avgExamScoreRate(totalMaxScoreSum == 0 ? 0.0 : totalExamScoreSum / totalMaxScoreSum)
                .mistakeCount(mistakes.size())
                .recentExams(recentExams.stream()
                        .sorted(Comparator.comparing(StudentAnalysisVO.RecentExamVO::getSubmitTime).reversed())
                        .limit(10) // 仅返回最近10场
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
                .examTypeStats(new ArrayList<>()) // 简化，暂不实现复杂的对比
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

        // 2. 获取考试记录 (包含已提交和已批阅)
        LambdaQueryWrapper<ExamRecord> recordQw = new LambdaQueryWrapper<ExamRecord>()
                .in(ExamRecord::getUserId, studentIds)
                .ge(ExamRecord::getStatus, 1); // 1-已提交, 2-已批阅

        if (examId != null) {
            recordQw.eq(ExamRecord::getExamId, examId);
        }

        List<ExamRecord> examRecords = examRecordMapper.selectList(recordQw);

        if (examRecords.isEmpty()) {
            return ClassAnalysisVO.builder()
                    .scoreDistribution(Collections.emptyList())
                    .averageScore(0.0)
                    .questionAccuracies(Collections.emptyList())
                    .build();
        }

        // 3. 统计动态分数分布 (根据每次考试各自的满分比例)
        int[] distribution = new int[5]; // 90-100, 80-89, 70-79, 60-69, 0-59
        double totalScoreSum = 0;

        // 缓存试卷满分，避免重复查询
        Map<Long, Integer> examMaxScoreMap = new HashMap<>();

        // 如果是单场考试，获取其满分
        Integer singleExamMaxScore = null;
        if (examId != null) {
            Exam exam = examMapper.selectById(examId);
            if (exam != null) {
                Paper paper = paperMapper.selectById(exam.getPaperId());
                singleExamMaxScore = (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;
            }
        }

        for (ExamRecord er : examRecords) {
            totalScoreSum += er.getTotalScore();

            Integer maxScore = examMaxScoreMap.computeIfAbsent(er.getExamId(), id -> {
                Exam exam = examMapper.selectById(id);
                if (exam != null) {
                    Paper paper = paperMapper.selectById(exam.getPaperId());
                    return (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;
                }
                return 100;
            });

            double ratio = (double) er.getTotalScore() / maxScore;
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

        // 构建标签，显示得分率区间
        String[] labels = { "90-100%", "80-89%", "70-79%", "60-69%", "0-59%" };

        List<ClassAnalysisVO.ScoreDistributionVO> distVOs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            distVOs.add(new ClassAnalysisVO.ScoreDistributionVO(labels[i], distribution[i]));
        }

        // 4. 统计题目正确率
        List<Long> recordIds = examRecords.stream().map(ExamRecord::getId).collect(Collectors.toList());

        // 如果指定了考试，则只统计该考试对应试卷的题目
        Set<Long> targetQuestionIds = new HashSet<>();
        if (examId != null) {
            Exam exam = examMapper.selectById(examId);
            if (exam != null) {
                List<PaperQuestion> pqs = paperQuestionMapper.selectList(
                        new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, exam.getPaperId()));
                targetQuestionIds = pqs.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toSet());
            }
        }

        List<AnswerDetail> details = answerDetailMapper.selectList(
                new LambdaQueryWrapper<AnswerDetail>().in(AnswerDetail::getExamRecordId, recordIds));

        Map<Long, List<Boolean>> questionMap = new HashMap<>();
        for (AnswerDetail detail : details) {
            // 如果筛选了考试，则只统计目标题目
            if (!targetQuestionIds.isEmpty() && !targetQuestionIds.contains(detail.getQuestionId())) {
                continue;
            }
            questionMap.putIfAbsent(detail.getQuestionId(), new ArrayList<>());
            questionMap.get(detail.getQuestionId()).add(detail.getIsCorrect() == 1);
        }

        List<ClassAnalysisVO.QuestionAccuracyVO> accuracyVOs = new ArrayList<>();
        if (!questionMap.isEmpty()) {
            List<Question> questions = questionMapper.selectBatchIds(questionMap.keySet());
            Map<Long, String> questionContentMap = questions.stream()
                    .collect(Collectors.toMap(Question::getId, Question::getContent));

            for (Map.Entry<Long, List<Boolean>> entry : questionMap.entrySet()) {
                String content = questionContentMap.getOrDefault(entry.getKey(), "未知题目");
                List<Boolean> results = entry.getValue();
                double accuracy = results.isEmpty() ? 0.0
                        : (double) results.stream().filter(Boolean.TRUE::equals).count() / results.size();
                accuracyVOs.add(new ClassAnalysisVO.QuestionAccuracyVO(entry.getKey(), content, accuracy));
            }
            // 按 ID 排序，保证稳定性
            accuracyVOs.sort(Comparator.comparing(ClassAnalysisVO.QuestionAccuracyVO::getQuestionId));
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
        // 使用更通用的 getAuthorities 检查角色，避免依赖具体的 roleCodes 字段
        boolean isAdmin = loginUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (isAdmin) {
            return classMapper.selectList(null);
        }

        return classMapper.selectList(new LambdaQueryWrapper<com.example.zaixiantiku.entity.Class>()
                .eq(com.example.zaixiantiku.entity.Class::getTeacherId, loginUser.getUser().getId()));
    }

    @Override
    public List<Exam> getClassExams(Long classId) {
        // 1. 获取班级所有学生
        List<StudentClass> studentsInClass = studentClassMapper.selectList(
                new LambdaQueryWrapper<StudentClass>().eq(StudentClass::getClassId, classId));
        if (studentsInClass.isEmpty())
            return Collections.emptyList();
        List<Long> studentIds = studentsInClass.stream().map(StudentClass::getStudentId).collect(Collectors.toList());

        // 2. 汇总考试 ID (从两个来源：1. 显式关联表 2. 实际考试记录)
        Set<Long> examIds = new HashSet<>();

        // 来源1：exam_class 关联表
        List<ExamClass> examClasses = examClassMapper.selectList(
                new LambdaQueryWrapper<ExamClass>().eq(ExamClass::getClassId, classId));
        examIds.addAll(examClasses.stream().map(ExamClass::getExamId).collect(Collectors.toSet()));

        // 来源2：该班级学生实际参加过的考试记录 (更可靠)
        List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .in(ExamRecord::getUserId, studentIds)
                .select(ExamRecord::getExamId));
        examIds.addAll(records.stream().map(ExamRecord::getExamId).collect(Collectors.toSet()));

        if (examIds.isEmpty()) {
            return Collections.emptyList();
        }

        return examMapper.selectList(new LambdaQueryWrapper<Exam>()
                .in(Exam::getId, examIds)
                .orderByDesc(Exam::getStartTime));
    }

    @Override
    public List<com.example.zaixiantiku.entity.Class> getAllClasses() {
        return classMapper.selectList(null);
    }

    @Override
    public GlobalAnalysisVO getGlobalAnalysis() {
        // 1. 获取所有考试记录 (不设限，确保统计到所有历史数据)
        List<ExamRecord> allRecords = examRecordMapper.selectList(null);

        // 2. 汇总考试信息 (从数据库和现有记录中)
        List<Exam> allExamsInDb = examMapper.selectList(null);
        Map<Long, Exam> examMap = allExamsInDb.stream().collect(Collectors.toMap(Exam::getId, e -> e));

        // 统计所有涉及到的考试 ID (包含记录中的 ID，防止考试被删除但记录还在)
        Set<Long> involvedExamIds = new HashSet<>();
        allExamsInDb.forEach(e -> involvedExamIds.add(e.getId()));
        allRecords.forEach(r -> {
            if (r.getExamId() != null) {
                involvedExamIds.add(r.getExamId());
            }
        });

        if (allRecords.isEmpty()) {
            return GlobalAnalysisVO.builder()
                    .totalExams(involvedExamIds.size())
                    .totalStudents(studentClassMapper.selectCount(null).intValue())
                    .averageScore(0.0)
                    .passRate(0.0)
                    .classPerformance(Collections.emptyList())
                    .build();
        }

        // 批量加载试卷信息
        Set<Long> paperIds = allExamsInDb.stream().map(Exam::getPaperId).collect(Collectors.toSet());
        final Map<Long, Paper> paperMap = paperIds.isEmpty() ? new HashMap<>()
                : paperMapper.selectBatchIds(paperIds).stream().collect(Collectors.toMap(Paper::getId, p -> p));

        // 3. 统计全局指标 (仅统计有分数的记录作为平均分和及格率的依据)
        List<ExamRecord> validRecords = allRecords.stream()
                .filter(r -> r.getTotalScore() != null)
                .collect(Collectors.toList());

        int totalRecords = validRecords.size();
        double totalScoreSum = validRecords.stream().mapToInt(ExamRecord::getTotalScore).sum();

        long passCount = validRecords.stream().filter(er -> {
            Exam exam = examMap.get(er.getExamId());
            int maxScore = 100;
            if (exam != null) {
                Paper paper = paperMap.get(exam.getPaperId());
                maxScore = (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;
            }
            return er.getTotalScore() >= maxScore * 0.6;
        }).count();

        // 4. 统计班级对比
        List<com.example.zaixiantiku.entity.Class> allClasses = classMapper.selectList(null);
        List<GlobalAnalysisVO.ClassPerformanceVO> performances = new ArrayList<>();

        List<StudentClass> allStudentClasses = studentClassMapper.selectList(null);
        Map<Long, List<Long>> classToStudentsMap = allStudentClasses.stream()
                .collect(Collectors.groupingBy(StudentClass::getClassId,
                        Collectors.mapping(StudentClass::getStudentId, Collectors.toList())));

        for (com.example.zaixiantiku.entity.Class cls : allClasses) {
            List<Long> studentIds = classToStudentsMap.get(cls.getId());
            if (studentIds == null || studentIds.isEmpty())
                continue;

            Set<Long> studentIdSet = new HashSet<>(studentIds);
            List<ExamRecord> classRecords = validRecords.stream()
                    .filter(er -> studentIdSet.contains(er.getUserId()))
                    .collect(Collectors.toList());

            if (classRecords.isEmpty())
                continue;

            double avgScore = classRecords.stream().mapToInt(ExamRecord::getTotalScore).average().orElse(0.0);
            long classPassCount = classRecords.stream().filter(er -> {
                Exam exam = examMap.get(er.getExamId());
                int maxScore = 100;
                if (exam != null) {
                    Paper paper = paperMap.get(exam.getPaperId());
                    maxScore = (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;
                }
                return er.getTotalScore() >= maxScore * 0.6;
            }).count();

            performances.add(GlobalAnalysisVO.ClassPerformanceVO.builder()
                    .classId(cls.getId())
                    .className(cls.getClassName())
                    .averageScore(avgScore)
                    .passRate((double) classPassCount / classRecords.size())
                    .build());
        }

        // 5. 统计考试概览
        List<GlobalAnalysisVO.ExamBriefVO> examBriefs = new ArrayList<>();
        Map<Long, List<ExamRecord>> examToRecordsMap = validRecords.stream()
                .collect(Collectors.groupingBy(ExamRecord::getExamId));

        for (Exam exam : allExamsInDb) {
            Paper paper = paperMap.get(exam.getPaperId());
            int maxScore = (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;

            List<ExamRecord> records = examToRecordsMap.get(exam.getId());
            if (records == null || records.isEmpty()) {
                examBriefs.add(GlobalAnalysisVO.ExamBriefVO.builder()
                        .id(exam.getId())
                        .examName(exam.getExamName())
                        .averageScore(0.0)
                        .maxScore(maxScore)
                        .passRate(0.0)
                        .participantCount(0)
                        .build());
                continue;
            }

            double avgScore = records.stream().mapToInt(ExamRecord::getTotalScore).average().orElse(0.0);
            long passCountInExam = records.stream().filter(er -> {
                return er.getTotalScore() >= maxScore * 0.6;
            }).count();

            examBriefs.add(GlobalAnalysisVO.ExamBriefVO.builder()
                    .id(exam.getId())
                    .examName(exam.getExamName())
                    .averageScore(avgScore)
                    .maxScore(maxScore)
                    .passRate((double) passCountInExam / records.size())
                    .participantCount(records.size())
                    .build());
        }

        return GlobalAnalysisVO.builder()
                .totalExams(involvedExamIds.size())
                .totalStudents((int) allStudentClasses.stream().map(StudentClass::getStudentId).distinct().count())
                .averageScore(totalScoreSum / totalRecords)
                .passRate((double) passCount / totalRecords)
                .classPerformance(performances)
                .recentExams(examBriefs)
                .build();
    }

    private Long getUserId() {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return loginUser.getUser().getId();
    }
}
