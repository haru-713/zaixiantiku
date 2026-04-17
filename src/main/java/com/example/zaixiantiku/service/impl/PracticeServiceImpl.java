package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.zaixiantiku.entity.*;
import com.example.zaixiantiku.mapper.*;
import com.example.zaixiantiku.pojo.dto.PracticeStartDTO;
import com.example.zaixiantiku.pojo.dto.PracticeSubmitDTO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.QuestionDetailVO;
import com.example.zaixiantiku.security.LoginUser;
import com.example.zaixiantiku.service.PracticeService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PracticeServiceImpl implements PracticeService {

    private final QuestionMapper questionMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final MistakeBookMapper mistakeBookMapper;
    private final FavoriteMapper favoriteMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startPractice(PracticeStartDTO startDTO) {
        LoginUser loginUser = getLoginUser();
        Long userId = loginUser.getUser().getId();

        // 1. 根据配置筛选题目
        LambdaQueryWrapper<Question> qw = new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, startDTO.getCourseId())
                .eq(Question::getStatus, 2); // 已发布

        if (startDTO.getConfig() != null) {
            if (startDTO.getConfig().getTypeIds() != null && !startDTO.getConfig().getTypeIds().isEmpty()) {
                qw.in(Question::getTypeId, startDTO.getConfig().getTypeIds());
            }
            if (startDTO.getConfig().getDifficulties() != null && !startDTO.getConfig().getDifficulties().isEmpty()) {
                qw.in(Question::getDifficulty, startDTO.getConfig().getDifficulties());
            }
            if (startDTO.getConfig().getKnowledgeIds() != null && !startDTO.getConfig().getKnowledgeIds().isEmpty()) {
                // 通过关联表查询题目ID
                List<QuestionKnowledge> qkList = questionKnowledgeMapper.selectList(
                        new LambdaQueryWrapper<QuestionKnowledge>()
                                .in(QuestionKnowledge::getKnowledgePointId, startDTO.getConfig().getKnowledgeIds()));

                if (qkList.isEmpty()) {
                    // 如果选了知识点但没找到题目，直接抛异常或让后续查询查不到
                    qw.in(Question::getId, Collections.singletonList(-1L));
                } else {
                    List<Long> qIds = qkList.stream().map(QuestionKnowledge::getQuestionId)
                            .collect(Collectors.toList());
                    qw.in(Question::getId, qIds);
                }
            }
        }

        List<Question> questions = questionMapper.selectList(qw);
        if (questions.isEmpty()) {
            throw new RuntimeException("未找到符合条件的题目");
        }
        Collections.shuffle(questions);

        // 限制最大题目数量为 50，最小为 1
        int requestedCount = (startDTO.getConfig() != null && startDTO.getConfig().getQuestionCount() != null)
                ? startDTO.getConfig().getQuestionCount()
                : 10;

        int count = Math.max(1, Math.min(Math.min(requestedCount, 50), questions.size()));

        List<Question> selected = questions.subList(0, count);
        List<Long> questionIds = selected.stream().map(Question::getId).collect(Collectors.toList());

        // 2. 创建练习记录
        PracticeRecord record = PracticeRecord.builder()
                .userId(userId)
                .courseId(startDTO.getCourseId())
                .questionIds(questionIds)
                .answers(new ArrayList<>())
                .startTime(LocalDateTime.now())
                .build();
        practiceRecordMapper.insert(record);

        // 3. 返回题目信息（脱敏答案）
        Map<String, Object> res = new HashMap<>();
        res.put("practiceId", record.getId());
        res.put("questions", selected.stream().map(this::toVO).collect(Collectors.toList()));
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitPractice(Long practiceId, PracticeSubmitDTO submitDTO) {
        PracticeRecord record = practiceRecordMapper.selectById(practiceId);
        if (record == null)
            throw new RuntimeException("练习记录不存在");

        LoginUser loginUser = getLoginUser();
        if (!record.getUserId().equals(loginUser.getUser().getId()))
            throw new RuntimeException("无权操作");

        List<Long> questionIds = record.getQuestionIds();
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        int totalScore = 0;
        int totalDuration = 0;
        List<Map<String, Object>> details = new ArrayList<>();
        List<Map<String, Object>> answers = new ArrayList<>();

        for (PracticeSubmitDTO.AnswerItem item : submitDTO.getAnswers()) {
            Question q = questionMap.get(item.getQuestionId());
            if (q == null)
                continue;

            boolean isCorrect = isAnswerCorrect(q.getAnswer(), item.getAnswer());
            if (isCorrect)
                totalScore += 10; // 假设每题10分

            totalDuration += (item.getTimeSpent() != null ? item.getTimeSpent() : 0);

            Map<String, Object> detail = new HashMap<>();
            detail.put("questionId", q.getId());
            detail.put("isCorrect", isCorrect);
            detail.put("correctAnswer", q.getAnswer());
            detail.put("analysis", q.getAnalysis());
            details.add(detail);

            Map<String, Object> answer = new HashMap<>();
            answer.put("questionId", q.getId());
            answer.put("userAnswer", item.getAnswer());
            answer.put("isCorrect", isCorrect);
            answer.put("timeSpent", item.getTimeSpent());
            answers.add(answer);

            // 记录错题或移除已掌握题目
            if (!isCorrect) {
                recordMistake(record.getUserId(), q.getId(), record.getCourseId());
            } else {
                // 如果答对了，尝试从错题本中移除（如果存在）
                mistakeBookMapper.delete(new LambdaQueryWrapper<MistakeBook>()
                        .eq(MistakeBook::getUserId, record.getUserId())
                        .eq(MistakeBook::getQuestionId, q.getId()));
            }
        }

        record.setAnswers(answers);
        record.setTotalScore(totalScore);
        record.setTotalDuration(totalDuration);
        record.setSubmitTime(LocalDateTime.now());
        practiceRecordMapper.updateById(record);

        Map<String, Object> res = new HashMap<>();
        res.put("totalScore", totalScore);
        res.put("details", details);
        return res;
    }

    private void recordMistake(Long userId, Long questionId, Long courseId) {
        // 使用 LambdaQueryWrapper 确保查询条件准确
        List<MistakeBook> list = mistakeBookMapper.selectList(new LambdaQueryWrapper<MistakeBook>()
                .eq(MistakeBook::getUserId, userId)
                .eq(MistakeBook::getQuestionId, questionId));

        if (list.isEmpty()) {
            MistakeBook mb = MistakeBook.builder()
                    .userId(userId)
                    .questionId(questionId)
                    .courseId(courseId)
                    .wrongCount(1)
                    .lastWrongTime(LocalDateTime.now())
                    .createTime(LocalDateTime.now())
                    .build();
            mistakeBookMapper.insert(mb);
        } else {
            // 如果存在多条（异常情况），取第一条更新，删除其他重复项
            MistakeBook mb = list.get(0);
            mb.setWrongCount(mb.getWrongCount() + 1);
            mb.setLastWrongTime(LocalDateTime.now());
            mistakeBookMapper.updateById(mb);

            if (list.size() > 1) {
                for (int i = 1; i < list.size(); i++) {
                    mistakeBookMapper.deleteById(list.get(i).getId());
                }
            }
        }
    }

    @Override
    public PageResult<PracticeRecord> getPracticeRecords(Integer page, Integer size) {
        LoginUser loginUser = getLoginUser();
        PageHelper.startPage(page, size);
        List<PracticeRecord> list = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, loginUser.getUser().getId())
                .orderByDesc(PracticeRecord::getSubmitTime));
        PageInfo<PracticeRecord> pageInfo = new PageInfo<>(list);
        return PageResult.of(pageInfo.getTotal(), list);
    }

    @Override
    public List<Map<String, Object>> getMistakeBook(Long courseId, Integer typeId) {
        LoginUser loginUser = getLoginUser();
        LambdaQueryWrapper<MistakeBook> qw = new LambdaQueryWrapper<MistakeBook>()
                .eq(MistakeBook::getUserId, loginUser.getUser().getId());
        if (courseId != null)
            qw.eq(MistakeBook::getCourseId, courseId);

        List<MistakeBook> mistakes = mistakeBookMapper.selectList(qw);
        if (mistakes.isEmpty())
            return new ArrayList<>();

        List<Long> qIds = mistakes.stream().map(MistakeBook::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> qMap = questionMapper.selectBatchIds(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        return mistakes.stream().map(m -> {
            Question q = qMap.get(m.getQuestionId());
            if (typeId != null && q != null && !q.getTypeId().equals(typeId))
                return null;

            Map<String, Object> item = new HashMap<>();
            item.put("id", m.getId());
            item.put("question", toVO(q));
            item.put("wrongCount", m.getWrongCount());
            item.put("note", m.getNote());
            item.put("lastWrongTime", m.getLastWrongTime());
            return item;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> redoMistake(Long mistakeId) {
        MistakeBook mb = mistakeBookMapper.selectById(mistakeId);
        if (mb == null)
            throw new RuntimeException("错题不存在");
        Question q = questionMapper.selectById(mb.getQuestionId());

        Map<String, Object> res = new HashMap<>();
        res.put("practiceId", 0); // 重做单题不创建正式练习记录
        res.put("questions", Collections.singletonList(toVO(q)));
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitRedo(Long mistakeId, String answer) {
        MistakeBook mb = mistakeBookMapper.selectById(mistakeId);
        if (mb == null)
            throw new RuntimeException("错题记录不存在");
        Question q = questionMapper.selectById(mb.getQuestionId());
        if (q == null)
            throw new RuntimeException("题目不存在");

        boolean isCorrect = isAnswerCorrect(q.getAnswer(), answer);
        if (isCorrect) {
            // 答对，从错题本中彻底移除该题（防止由于之前逻辑产生的重复记录）
            mistakeBookMapper.delete(new LambdaQueryWrapper<MistakeBook>()
                    .eq(MistakeBook::getUserId, mb.getUserId())
                    .eq(MistakeBook::getQuestionId, mb.getQuestionId()));
        } else {
            // 答错，增加错误次数
            mb.setWrongCount(mb.getWrongCount() + 1);
            mb.setLastWrongTime(LocalDateTime.now());
            mistakeBookMapper.updateById(mb);
        }
        return isCorrect;
    }

    @Override
    public void removeMistake(Long mistakeId) {
        mistakeBookMapper.deleteById(mistakeId);
    }

    @Override
    public MistakeBook updateMistakeNote(Long mistakeId, String note) {
        MistakeBook mb = mistakeBookMapper.selectById(mistakeId);
        if (mb != null) {
            mb.setNote(note);
            mistakeBookMapper.updateById(mb);
        }
        return mb;
    }

    @Override
    public void addFavorite(Long questionId) {
        LoginUser loginUser = getLoginUser();
        Favorite fav = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, loginUser.getUser().getId())
                .eq(Favorite::getQuestionId, questionId));
        if (fav == null) {
            fav = Favorite.builder()
                    .userId(loginUser.getUser().getId())
                    .questionId(questionId)
                    .createTime(LocalDateTime.now())
                    .build();
            favoriteMapper.insert(fav);
        }
    }

    @Override
    public void removeFavorite(Long favoriteId) {
        favoriteMapper.deleteById(favoriteId);
    }

    @Override
    public PageResult<QuestionDetailVO> getFavorites(Integer page, Integer size) {
        LoginUser loginUser = getLoginUser();
        PageHelper.startPage(page, size);
        List<Favorite> favs = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, loginUser.getUser().getId())
                .orderByDesc(Favorite::getCreateTime));
        PageInfo<Favorite> pageInfo = new PageInfo<>(favs);

        if (favs.isEmpty())
            return PageResult.of(0L, new ArrayList<>());

        List<Long> qIds = favs.stream().map(Favorite::getQuestionId).collect(Collectors.toList());
        List<Question> questions = questionMapper.selectBatchIds(qIds);

        return PageResult.of(pageInfo.getTotal(), questions.stream().map(this::toVO).collect(Collectors.toList()));
    }

    private boolean isAnswerCorrect(String standard, String user) {
        if (standard == null || user == null)
            return false;

        String uNormal = normalizeString(user.toUpperCase());

        // 支持填空题多个正确答案（以 / 分隔）
        String[] standards = standard.split("/");
        for (String s : standards) {
            String sNormal = normalizeString(s.toUpperCase());

            // 1. 基础清理后完全一致
            if (sNormal.equals(uNormal))
                return true;

            // 2. 多选题乱序对比（针对 ABC 这种没有逗号的情况）
            if (sNormal.length() > 1 && sNormal.matches("^[A-Z]+$") && uNormal.matches("^[A-Z]+$")) {
                if (sNormal.length() == uNormal.length()) {
                    char[] sChars = sNormal.toCharArray();
                    char[] uChars = uNormal.toCharArray();
                    Arrays.sort(sChars);
                    Arrays.sort(uChars);
                    if (Arrays.equals(sChars, uChars))
                        return true;
                }
            }
        }

        return false;
    }

    private String normalizeString(String str) {
        if (str == null)
            return "";
        // 移除所有空白字符，并将中文常用标点转为英文
        return str.replaceAll("\\s+", "")
                .replace("，", ",")
                .replace("。", ".")
                .replace("！", "!")
                .replace("？", "?")
                .replace("（", "(")
                .replace("）", ")");
    }

    private QuestionDetailVO toVO(Question q) {
        if (q == null)
            return null;
        return QuestionDetailVO.builder()
                .id(q.getId())
                .courseId(q.getCourseId())
                .typeId(q.getTypeId().intValue())
                .content(q.getContent())
                .difficulty(q.getDifficulty().intValue())
                .options(parseOptions(q.getOptions()))
                .answer(q.getAnswer())
                .analysis(q.getAnalysis())
                .status(q.getStatus().intValue())
                .createTime(q.getCreateTime())
                .build();
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

    private LoginUser getLoginUser() {
        return (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
