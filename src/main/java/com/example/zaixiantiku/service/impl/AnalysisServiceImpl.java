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
    public StudentAnalysisVO getStudentAnalysisReport(Long courseId) {
        Long userId = getUserId();

        // 1. 获取练习记录
        LambdaQueryWrapper<PracticeRecord> practiceQw = new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .isNotNull(PracticeRecord::getSubmitTime);
        if (courseId != null) {
            practiceQw.eq(PracticeRecord::getCourseId, courseId);
        }
        List<PracticeRecord> practiceRecords = practiceRecordMapper.selectList(practiceQw);

        // 2. 获取考试记录（包含已提交和已批阅）
        LambdaQueryWrapper<ExamRecord> examQw = new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .ge(ExamRecord::getStatus, 1); // 1-已提交, 2-已批阅
        List<ExamRecord> examRecords = examRecordMapper.selectList(examQw);
        // 如果指定了课程，过滤考试记录（ExamRecord 没有 courseId，需通过 Exam 关联）
        if (courseId != null) {
            examRecords = examRecords.stream().filter(er -> {
                Exam exam = examMapper.selectById(er.getExamId());
                return exam != null && courseId.equals(exam.getCourseId());
            }).collect(Collectors.toList());
        }

        // 3. 统计基础数据
        int totalPracticeCount = practiceRecords.size() + examRecords.size();
        int totalQuestionCount = 0;
        int totalCorrectCount = 0;

        // 趋势统计: 按日期分组
        Map<String, List<Boolean>> trendMap = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 知识点掌握度统计
        Map<Long, List<Boolean>> knowledgeMap = new HashMap<>();
        // 建立 题目ID -> 是否正确 的快速映射，用于后续关联知识点
        Map<Long, List<Boolean>> qIdToResults = new HashMap<>();

        // 处理练习记录
        for (PracticeRecord record : practiceRecords) {
            String date = record.getSubmitTime().format(formatter);
            trendMap.putIfAbsent(date, new ArrayList<>());

            List<Map<String, Object>> answers = record.getAnswers();
            if (answers != null && !answers.isEmpty()) {
                for (Map<String, Object> ans : answers) {
                    if (ans.get("questionId") == null)
                        continue;
                    Long qId = Long.valueOf(ans.get("questionId").toString());
                    boolean isCorrect = checkCorrect(ans.get("isCorrect"));

                    totalQuestionCount++;
                    if (isCorrect)
                        totalCorrectCount++;
                    trendMap.get(date).add(isCorrect);

                    qIdToResults.putIfAbsent(qId, new ArrayList<>());
                    qIdToResults.get(qId).add(isCorrect);
                }
            } else if (record.getQuestionIds() != null && !record.getQuestionIds().isEmpty()) {
                int qCount = record.getQuestionIds().size();
                int score = record.getTotalScore() != null ? record.getTotalScore() : 0;
                int estCorrect = Math.min(qCount, score / 10); // 估算
                totalQuestionCount += qCount;
                totalCorrectCount += estCorrect;
                for (int i = 0; i < estCorrect; i++)
                    trendMap.get(date).add(true);
                for (int i = 0; i < qCount - estCorrect; i++)
                    trendMap.get(date).add(false);

                // 老数据缺失具体题目对错，无法用于知识点分析，但已计入总数
            }
        }

        // 处理考试记录
        for (ExamRecord er : examRecords) {
            String date = er.getSubmitTime().format(formatter);
            trendMap.putIfAbsent(date, new ArrayList<>());

            List<AnswerDetail> details = answerDetailMapper.selectList(
                    new LambdaQueryWrapper<AnswerDetail>().eq(AnswerDetail::getExamRecordId, er.getId()));

            for (AnswerDetail detail : details) {
                boolean isCorrect = detail.getIsCorrect() == 1;
                totalQuestionCount++;
                if (isCorrect)
                    totalCorrectCount++;
                trendMap.get(date).add(isCorrect);

                qIdToResults.putIfAbsent(detail.getQuestionId(), new ArrayList<>());
                qIdToResults.get(detail.getQuestionId()).add(isCorrect);
            }
        }

        // 4. 统计知识点掌握度
        Set<Long> allParticipatedQuestionIds = qIdToResults.keySet();
        if (!allParticipatedQuestionIds.isEmpty()) {
            List<QuestionKnowledge> allQks = questionKnowledgeMapper.selectList(
                    new LambdaQueryWrapper<QuestionKnowledge>().in(QuestionKnowledge::getQuestionId,
                            allParticipatedQuestionIds));

            Map<Long, List<Long>> knowledgeToQuestionMap = allQks.stream()
                    .collect(Collectors.groupingBy(QuestionKnowledge::getKnowledgePointId,
                            Collectors.mapping(QuestionKnowledge::getQuestionId, Collectors.toList())));

            for (Map.Entry<Long, List<Long>> entry : knowledgeToQuestionMap.entrySet()) {
                Long kpId = entry.getKey();
                knowledgeMap.putIfAbsent(kpId, new ArrayList<>());
                for (Long qId : entry.getValue()) {
                    List<Boolean> results = qIdToResults.get(qId);
                    if (results != null)
                        knowledgeMap.get(kpId).addAll(results);
                }
            }
        }

        // 5. 统计错题本数量
        LambdaQueryWrapper<MistakeBook> mistakeQw = new LambdaQueryWrapper<MistakeBook>()
                .eq(MistakeBook::getUserId, userId);
        if (courseId != null) {
            mistakeQw.eq(MistakeBook::getCourseId, courseId);
        }
        Long mistakeCount = mistakeBookMapper.selectCount(mistakeQw);

        double avgAccuracy = totalQuestionCount == 0 ? 0.0 : (double) totalCorrectCount / totalQuestionCount;

        // 6. 构建 VO
        List<StudentAnalysisVO.TrendVO> trendVOs = trendMap.entrySet().stream()
                .map(e -> {
                    List<Boolean> results = e.getValue();
                    double accuracy = results.isEmpty() ? 0.0
                            : (double) results.stream().filter(Boolean.TRUE::equals).count() / results.size();
                    return new StudentAnalysisVO.TrendVO(e.getKey(), accuracy);
                })
                .collect(Collectors.toList());

        List<StudentAnalysisVO.KnowledgeMasteryVO> knowledgeVOs = new ArrayList<>();
        if (!knowledgeMap.isEmpty()) {
            List<KnowledgePoint> kps = knowledgePointMapper.selectBatchIds(knowledgeMap.keySet());
            Map<Long, String> kpNameMap = kps.stream()
                    .collect(Collectors.toMap(KnowledgePoint::getId, KnowledgePoint::getName));

            for (Map.Entry<Long, List<Boolean>> entry : knowledgeMap.entrySet()) {
                String name = kpNameMap.getOrDefault(entry.getKey(), "未知知识点");
                List<Boolean> results = entry.getValue();
                double accuracy = results.isEmpty() ? 0.0
                        : (double) results.stream().filter(Boolean.TRUE::equals).count() / results.size();
                knowledgeVOs.add(new StudentAnalysisVO.KnowledgeMasteryVO(name, accuracy));
            }
        }

        return StudentAnalysisVO.builder()
                .totalPracticeCount(totalPracticeCount)
                .totalQuestionCount(totalQuestionCount)
                .mistakeCount(mistakeCount.intValue())
                .avgAccuracy(avgAccuracy)
                .trend(trendVOs)
                .knowledgeMastery(knowledgeVOs)
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
        int[] distribution = new int[4];
        double totalScoreSum = 0;

        // 缓存试卷满分，避免重复查询
        Map<Long, Integer> examMaxScoreMap = new HashMap<>();

        // 如果是单场考试，获取其满分用于显示标签
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
            if (ratio >= 0.85)
                distribution[0]++;
            else if (ratio >= 0.7)
                distribution[1]++;
            else if (ratio >= 0.6)
                distribution[2]++;
            else
                distribution[3]++;
        }

        // 构建标签，如果单场考试则显示具体分值
        String[] labels = new String[4];
        if (singleExamMaxScore != null) {
            labels[0] = String.format("优秀 (>=%.1f分)", singleExamMaxScore * 0.85);
            labels[1] = String.format("良好 (%.1f-%.1f分)", singleExamMaxScore * 0.7, singleExamMaxScore * 0.84);
            labels[2] = String.format("及格 (%.1f-%.1f分)", singleExamMaxScore * 0.6, singleExamMaxScore * 0.69);
            labels[3] = String.format("不及格 (<%.1f分)", singleExamMaxScore * 0.6);
        } else {
            labels[0] = "优秀 (>=85%)";
            labels[1] = "良好 (70-84%)";
            labels[2] = "及格 (60-69%)";
            labels[3] = "不及格 (<60%)";
        }

        List<ClassAnalysisVO.ScoreDistributionVO> distVOs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
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
            List<ExamRecord> records = examToRecordsMap.get(exam.getId());
            if (records == null || records.isEmpty()) {
                examBriefs.add(GlobalAnalysisVO.ExamBriefVO.builder()
                        .id(exam.getId())
                        .examName(exam.getExamName())
                        .averageScore(0.0)
                        .passRate(0.0)
                        .participantCount(0)
                        .build());
                continue;
            }

            double avgScore = records.stream().mapToInt(ExamRecord::getTotalScore).average().orElse(0.0);
            long passCountInExam = records.stream().filter(er -> {
                Paper paper = paperMap.get(exam.getPaperId());
                int maxScore = (paper != null && paper.getTotalScore() != null) ? paper.getTotalScore() : 100;
                return er.getTotalScore() >= maxScore * 0.6;
            }).count();

            examBriefs.add(GlobalAnalysisVO.ExamBriefVO.builder()
                    .id(exam.getId())
                    .examName(exam.getExamName())
                    .averageScore(avgScore)
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
