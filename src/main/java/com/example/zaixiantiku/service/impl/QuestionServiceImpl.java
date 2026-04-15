package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.zaixiantiku.entity.Course;
import com.example.zaixiantiku.entity.CourseTeacher;
import com.example.zaixiantiku.entity.KnowledgePoint;
import com.example.zaixiantiku.entity.Question;
import com.example.zaixiantiku.entity.QuestionKnowledge;
import com.example.zaixiantiku.entity.QuestionType;
import com.example.zaixiantiku.mapper.CourseMapper;
import com.example.zaixiantiku.mapper.CourseTeacherMapper;
import com.example.zaixiantiku.mapper.KnowledgePointMapper;
import com.example.zaixiantiku.mapper.QuestionKnowledgeMapper;
import com.example.zaixiantiku.mapper.QuestionMapper;
import com.example.zaixiantiku.mapper.QuestionTypeMapper;
import com.example.zaixiantiku.pojo.dto.QuestionQueryDTO;
import com.example.zaixiantiku.pojo.dto.QuestionSaveDTO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.QuestionDetailVO;
import com.example.zaixiantiku.pojo.vo.QuestionListVO;
import com.example.zaixiantiku.security.LoginUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements com.example.zaixiantiku.service.QuestionService {

    private final QuestionMapper questionMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final QuestionTypeMapper questionTypeMapper;
    private final CourseMapper courseMapper;
    private final CourseTeacherMapper courseTeacherMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionDetailVO createQuestion(QuestionSaveDTO saveDTO) {
        validateSaveDTO(saveDTO);

        LoginUser loginUser = requireLoginUser();
        Long creatorId = loginUser.getUser().getId();
        requireTeacherOfCourseOrAdmin(loginUser, saveDTO.getCourseId());

        Question question = Question.builder()
                .courseId(saveDTO.getCourseId())
                .typeId(saveDTO.getTypeId())
                .content(saveDTO.getContent())
                .difficulty(saveDTO.getDifficulty())
                .options(toOptionsJson(saveDTO.getOptions()))
                .answer(saveDTO.getAnswer())
                .analysis(saveDTO.getAnalysis())
                .status(2)
                .createBy(creatorId)
                .build();

        int rows = questionMapper.insert(question);
        if (rows != 1 || question.getId() == null) {
            throw new RuntimeException("题目创建失败");
        }

        saveKnowledgeMappings(question.getId(), saveDTO.getKnowledgeIds(), saveDTO.getCourseId());
        return getQuestionDetail(question.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionDetailVO updateQuestion(Long questionId, QuestionSaveDTO saveDTO) {
        if (questionId == null) {
            throw new RuntimeException("questionId 不能为空");
        }
        validateSaveDTO(saveDTO);

        Question existing = questionMapper.selectById(questionId);
        if (existing == null) {
            throw new RuntimeException("题目不存在");
        }

        LoginUser loginUser = requireLoginUser();
        requireQuestionOwnerOrAdmin(loginUser, existing);
        requireTeacherOfCourseOrAdmin(loginUser, saveDTO.getCourseId());

        Question update = new Question();
        update.setId(questionId);
        update.setCourseId(saveDTO.getCourseId());
        update.setTypeId(saveDTO.getTypeId());
        update.setContent(saveDTO.getContent());
        update.setDifficulty(saveDTO.getDifficulty());
        update.setOptions(toOptionsJson(saveDTO.getOptions()));
        update.setAnswer(saveDTO.getAnswer());
        update.setAnalysis(saveDTO.getAnalysis());

        int rows = questionMapper.updateById(update);
        if (rows != 1) {
            throw new RuntimeException("题目修改失败");
        }

        questionKnowledgeMapper.delete(new LambdaQueryWrapper<QuestionKnowledge>().eq(QuestionKnowledge::getQuestionId, questionId));
        saveKnowledgeMappings(questionId, saveDTO.getKnowledgeIds(), saveDTO.getCourseId());
        return getQuestionDetail(questionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuestion(Long questionId) {
        if (questionId == null) {
            throw new RuntimeException("questionId 不能为空");
        }

        Question existing = questionMapper.selectById(questionId);
        if (existing == null) {
            throw new RuntimeException("题目不存在");
        }

        LoginUser loginUser = requireLoginUser();
        requireQuestionOwnerOrAdmin(loginUser, existing);
        ensureNotReferencedByPaper(questionId);

        int rows = questionMapper.deleteById(questionId);
        if (rows != 1) {
            throw new RuntimeException("删除失败");
        }
    }

    @Override
    public PageResult<QuestionListVO> getQuestionPage(QuestionQueryDTO queryDTO) {
        Integer page = queryDTO == null || queryDTO.getPage() == null || queryDTO.getPage() < 1 ? 1 : queryDTO.getPage();
        Integer size = queryDTO == null || queryDTO.getSize() == null || queryDTO.getSize() < 1 ? 10 : queryDTO.getSize();
        PageHelper.startPage(page, size);

        LambdaQueryWrapper<Question> qw = new LambdaQueryWrapper<>();
        if (queryDTO != null) {
            if (queryDTO.getCourseId() != null) {
                qw.eq(Question::getCourseId, queryDTO.getCourseId());
            }
            if (queryDTO.getTypeId() != null) {
                qw.eq(Question::getTypeId, queryDTO.getTypeId());
            }
            if (queryDTO.getDifficulty() != null) {
                qw.eq(Question::getDifficulty, queryDTO.getDifficulty());
            }
            if (queryDTO.getStatus() != null) {
                qw.eq(Question::getStatus, queryDTO.getStatus());
            }
            if (StringUtils.hasText(queryDTO.getKeyword())) {
                qw.like(Question::getContent, queryDTO.getKeyword());
            }
        }

        LoginUser loginUser = requireLoginUser();
        if (!isAdmin(loginUser)) {
            qw.eq(Question::getCreateBy, loginUser.getUser().getId());
        }

        if (queryDTO != null && queryDTO.getKnowledgeId() != null) {
            List<Long> ids = questionKnowledgeMapper.selectList(new LambdaQueryWrapper<QuestionKnowledge>()
                            .eq(QuestionKnowledge::getKnowledgePointId, queryDTO.getKnowledgeId()))
                    .stream()
                    .map(QuestionKnowledge::getQuestionId)
                    .distinct()
                    .toList();
            if (ids.isEmpty()) {
                return PageResult.of(0L, Collections.emptyList());
            }
            qw.in(Question::getId, ids);
        }

        qw.orderByDesc(Question::getId);
        List<Question> list = questionMapper.selectList(qw);
        PageInfo<Question> pageInfo = new PageInfo<>(list);

        List<QuestionListVO> voList = list.stream().map(q -> QuestionListVO.builder()
                .id(q.getId())
                .content(q.getContent())
                .typeId(q.getTypeId())
                .difficulty(q.getDifficulty())
                .status(q.getStatus())
                .createTime(q.getCreateTime())
                .build()).collect(Collectors.toList());

        return PageResult.of(pageInfo.getTotal(), voList);
    }

    @Override
    public QuestionDetailVO getQuestionDetail(Long questionId) {
        if (questionId == null) {
            throw new RuntimeException("questionId 不能为空");
        }

        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new RuntimeException("题目不存在");
        }

        LoginUser loginUser = requireLoginUser();
        if (!isAdmin(loginUser) && !Objects.equals(question.getCreateBy(), loginUser.getUser().getId())) {
            throw new RuntimeException("没有权限查看该题目");
        }

        List<Long> knowledgeIds = questionKnowledgeMapper.selectList(new LambdaQueryWrapper<QuestionKnowledge>()
                        .eq(QuestionKnowledge::getQuestionId, questionId))
                .stream()
                .map(QuestionKnowledge::getKnowledgePointId)
                .distinct()
                .collect(Collectors.toList());

        return QuestionDetailVO.builder()
                .id(question.getId())
                .courseId(question.getCourseId())
                .typeId(question.getTypeId())
                .content(question.getContent())
                .difficulty(question.getDifficulty())
                .options(parseOptions(question.getOptions()))
                .answer(question.getAnswer())
                .analysis(question.getAnalysis())
                .status(question.getStatus())
                .createBy(question.getCreateBy())
                .knowledgeIds(knowledgeIds)
                .createTime(question.getCreateTime())
                .updateTime(question.getUpdateTime())
                .build();
    }

    private void validateSaveDTO(QuestionSaveDTO saveDTO) {
        if (saveDTO == null) {
            throw new RuntimeException("请求体不能为空");
        }
        if (saveDTO.getCourseId() == null) {
            throw new RuntimeException("courseId 不能为空");
        }
        if (saveDTO.getTypeId() == null) {
            throw new RuntimeException("typeId 不能为空");
        }
        if (!StringUtils.hasText(saveDTO.getContent())) {
            throw new RuntimeException("content 不能为空");
        }
        if (saveDTO.getDifficulty() == null || saveDTO.getDifficulty() < 1 || saveDTO.getDifficulty() > 3) {
            throw new RuntimeException("difficulty 参数非法");
        }
        if (!StringUtils.hasText(saveDTO.getAnswer())) {
            throw new RuntimeException("answer 不能为空");
        }

        Course course = courseMapper.selectById(saveDTO.getCourseId());
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        QuestionType type = questionTypeMapper.selectById(saveDTO.getTypeId());
        if (type == null || (type.getStatus() != null && type.getStatus() == 0)) {
            throw new RuntimeException("题型不存在或已禁用");
        }

        validateKnowledgeIds(saveDTO.getKnowledgeIds(), saveDTO.getCourseId());
    }

    private void validateKnowledgeIds(List<Long> knowledgeIds, Long courseId) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return;
        }
        List<Long> ids = knowledgeIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return;
        }
        List<KnowledgePoint> kpList = knowledgePointMapper.selectBatchIds(ids);
        if (kpList.size() != ids.size()) {
            throw new RuntimeException("知识点不存在");
        }
        boolean ok = kpList.stream().allMatch(kp -> Objects.equals(kp.getCourseId(), courseId));
        if (!ok) {
            throw new RuntimeException("知识点与课程不匹配");
        }
    }

    private void saveKnowledgeMappings(Long questionId, List<Long> knowledgeIds, Long courseId) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return;
        }
        validateKnowledgeIds(knowledgeIds, courseId);
        Set<Long> uniq = new HashSet<>();
        for (Long kid : knowledgeIds) {
            if (kid == null || !uniq.add(kid)) {
                continue;
            }
            try {
                questionKnowledgeMapper.insert(QuestionKnowledge.builder()
                        .questionId(questionId)
                        .knowledgePointId(kid)
                        .build());
            } catch (DuplicateKeyException e) {
                continue;
            }
        }
    }

    private String toOptionsJson(List<String> options) {
        if (options == null) {
            return null;
        }
        List<String> cleaned = options.stream().filter(StringUtils::hasText).map(String::trim).toList();
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(cleaned);
        } catch (Exception e) {
            throw new RuntimeException("options 格式错误");
        }
    }

    private List<String> parseOptions(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private void requireTeacherOfCourseOrAdmin(LoginUser loginUser, Long courseId) {
        if (isAdmin(loginUser)) {
            return;
        }
        List<String> roles = loginUser.getRoleCodes();
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

    private void requireQuestionOwnerOrAdmin(LoginUser loginUser, Question question) {
        if (isAdmin(loginUser)) {
            return;
        }
        if (!Objects.equals(question.getCreateBy(), loginUser.getUser().getId())) {
            throw new RuntimeException("没有权限操作该题目");
        }
    }

    private boolean isAdmin(LoginUser loginUser) {
        List<String> roles = loginUser == null ? null : loginUser.getRoleCodes();
        return roles != null && roles.contains("ADMIN");
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null || loginUser.getUser() == null || loginUser.getUser().getId() == null) {
            throw new RuntimeException("未登录");
        }
        return loginUser;
    }

    private LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    private void ensureNotReferencedByPaper(Long questionId) {
        if (existsTable("paper_question")) {
            Integer c = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM paper_question WHERE question_id = ?", Integer.class, questionId);
            if (c != null && c > 0) {
                throw new RuntimeException("题目已被试卷引用，禁止删除");
            }
        }
        if (existsTable("exam_paper_question")) {
            Integer c = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM exam_paper_question WHERE question_id = ?", Integer.class, questionId);
            if (c != null && c > 0) {
                throw new RuntimeException("题目已被试卷引用，禁止删除");
            }
        }
    }

    private boolean existsTable(String tableName) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName
        );
        return c != null && c > 0;
    }
}

