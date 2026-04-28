package com.example.zaixiantiku.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.zaixiantiku.entity.Course;
import com.example.zaixiantiku.entity.CourseTeacher;
import com.example.zaixiantiku.entity.KnowledgePoint;
import com.example.zaixiantiku.entity.Question;
import com.example.zaixiantiku.entity.QuestionKnowledge;
import com.example.zaixiantiku.entity.QuestionType;
import com.example.zaixiantiku.entity.User;
import com.example.zaixiantiku.mapper.CourseMapper;
import com.example.zaixiantiku.mapper.CourseTeacherMapper;
import com.example.zaixiantiku.mapper.KnowledgePointMapper;
import com.example.zaixiantiku.mapper.QuestionKnowledgeMapper;
import com.example.zaixiantiku.mapper.QuestionMapper;
import com.example.zaixiantiku.mapper.QuestionTypeMapper;
import com.example.zaixiantiku.mapper.UserMapper;
import com.example.zaixiantiku.pojo.dto.QuestionAuditDTO;
import com.example.zaixiantiku.pojo.dto.QuestionImportDTO;
import com.example.zaixiantiku.pojo.dto.QuestionQueryDTO;
import com.example.zaixiantiku.pojo.dto.QuestionSaveDTO;
import com.example.zaixiantiku.pojo.vo.ImportResultVO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.QuestionDetailVO;
import com.example.zaixiantiku.pojo.vo.QuestionListVO;
import com.example.zaixiantiku.security.LoginUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.zaixiantiku.utils.RedisUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements com.example.zaixiantiku.service.QuestionService {

    private final QuestionMapper questionMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final QuestionTypeMapper questionTypeMapper;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final CourseTeacherMapper courseTeacherMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final RedisUtils redisUtils;

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
                .status(1) // 修改：初始状态设为 1 (待审核)，需管理员审核后方可发布 (status=2)
                .createBy(creatorId)
                .build();

        int rows = questionMapper.insert(question);
        if (rows != 1 || question.getId() == null) {
            throw new RuntimeException("题目创建失败");
        }

        saveKnowledgeMappings(question.getId(), saveDTO.getKnowledgeIds(), saveDTO.getCourseId());
        // 清除缓存
        clearQuestionCache();
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

        questionKnowledgeMapper
                .delete(new LambdaQueryWrapper<QuestionKnowledge>().eq(QuestionKnowledge::getQuestionId, questionId));
        saveKnowledgeMappings(questionId, saveDTO.getKnowledgeIds(), saveDTO.getCourseId());
        // 清除缓存
        clearQuestionCache();
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
        // 清除缓存
        clearQuestionCache();
    }

    @Override
    public PageResult<QuestionListVO> getQuestionPage(QuestionQueryDTO queryDTO) {
        // 1. 生成缓存 Key（根据角色和查询参数 DTO 序列化）
        LoginUser loginUser = requireLoginUser();
        String rolePrefix = isAdmin(loginUser) ? "admin" : "teacher:" + loginUser.getUser().getId();
        String cacheKey = "question:page:" + rolePrefix + ":" + (queryDTO == null ? "default"
                : queryDTO.getPage() + ":" + queryDTO.getSize() + ":" + queryDTO.getCourseId() + ":"
                        + queryDTO.getTypeId() + ":" + queryDTO.getDifficulty() + ":"
                        + queryDTO.getKnowledgeId() + ":" + queryDTO.getStatus() + ":"
                        + queryDTO.getKeyword());

        // 2. 尝试从缓存获取
        PageResult<QuestionListVO> cachedResult = redisUtils.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        Integer page = queryDTO == null || queryDTO.getPage() == null || queryDTO.getPage() < 1 ? 1
                : queryDTO.getPage();
        Integer size = queryDTO == null || queryDTO.getSize() == null || queryDTO.getSize() < 1 ? 10
                : queryDTO.getSize();

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

        if (!isAdmin(loginUser)) {
            List<Long> courseIds = courseTeacherMapper.selectList(new LambdaQueryWrapper<CourseTeacher>()
                    .eq(CourseTeacher::getTeacherId, loginUser.getUser().getId()))
                    .stream()
                    .map(CourseTeacher::getCourseId)
                    .toList();
            if (courseIds.isEmpty()) {
                return PageResult.of(0L, Collections.emptyList());
            }
            qw.in(Question::getCourseId, courseIds);
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

        // PageHelper 必须在紧邻的查询之前启动，否则会被中间的查询（如权限查询或知识点查询）截断
        PageHelper.startPage(page, size);
        qw.orderByDesc(Question::getId);
        List<Question> list = questionMapper.selectList(qw);
        PageInfo<Question> pageInfo = new PageInfo<>(list);

        if (list.isEmpty()) {
            return PageResult.of(pageInfo.getTotal(), Collections.emptyList());
        }

        // 批量查询课程和用户信息
        Set<Long> courseIds = list.stream().map(Question::getCourseId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> userIds = list.stream().map(Question::getCreateBy).filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> courseNameMap = courseIds.isEmpty() ? Collections.emptyMap()
                : courseMapper.selectBatchIds(courseIds).stream()
                        .collect(Collectors.toMap(Course::getId, Course::getCourseName));
        Map<Long, String> userNameMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId,
                                u -> StringUtils.hasText(u.getName()) ? u.getName() : u.getUsername()));

        // 批量查询知识点关联
        Set<Long> questionIds = list.stream().map(Question::getId).collect(Collectors.toSet());
        Map<Long, List<String>> questionKnowledgeMap = new HashMap<>();
        if (!questionIds.isEmpty()) {
            // 1. 获取所有关联
            List<QuestionKnowledge> allRelations = questionKnowledgeMapper
                    .selectList(new LambdaQueryWrapper<QuestionKnowledge>()
                            .in(QuestionKnowledge::getQuestionId, questionIds));

            if (!allRelations.isEmpty()) {
                // 2. 获取所有知识点名称
                Set<Long> kPointIds = allRelations.stream().map(QuestionKnowledge::getKnowledgePointId)
                        .collect(Collectors.toSet());
                Map<Long, String> kPointNameMap = knowledgePointMapper.selectBatchIds(kPointIds).stream()
                        .collect(Collectors.toMap(KnowledgePoint::getId, KnowledgePoint::getName));

                // 3. 按照试题 ID 分组并填充名称
                allRelations.forEach(rel -> {
                    String name = kPointNameMap.get(rel.getKnowledgePointId());
                    if (name != null) {
                        questionKnowledgeMap.computeIfAbsent(rel.getQuestionId(), k -> new ArrayList<>()).add(name);
                    }
                });
            }
        }

        List<QuestionListVO> voList = list.stream().map(q -> {
            return QuestionListVO.builder()
                    .id(q.getId())
                    .content(q.getContent())
                    .courseName(courseNameMap.getOrDefault(q.getCourseId(), "未知课程"))
                    .creatorName(userNameMap.getOrDefault(q.getCreateBy(), "未知用户"))
                    .typeId(q.getTypeId())
                    .difficulty(q.getDifficulty())
                    .status(q.getStatus())
                    .knowledgeNames(questionKnowledgeMap.getOrDefault(q.getId(), Collections.emptyList()))
                    .createTime(q.getCreateTime())
                    .build();
        }).collect(Collectors.toList());

        PageResult<QuestionListVO> result = PageResult.of(pageInfo.getTotal(), voList);

        // 3. 存入缓存，设置 10 分钟过期（如果是热门数据可以设置更长）
        redisUtils.set(cacheKey, result, 10, java.util.concurrent.TimeUnit.MINUTES);

        return result;
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

        List<String> knowledgeNames = knowledgeIds.isEmpty() ? Collections.emptyList()
                : knowledgePointMapper.selectBatchIds(knowledgeIds).stream().map(KnowledgePoint::getName).toList();

        Course course = courseMapper.selectById(question.getCourseId());
        User creator = userMapper.selectById(question.getCreateBy());

        return QuestionDetailVO.builder()
                .id(question.getId())
                .courseId(question.getCourseId())
                .courseName(course != null ? course.getCourseName() : "未知课程")
                .typeId(question.getTypeId())
                .content(question.getContent())
                .difficulty(question.getDifficulty())
                .options(parseOptions(question.getOptions()))
                .answer(question.getAnswer())
                .analysis(question.getAnalysis())
                .status(question.getStatus())
                .createBy(question.getCreateBy())
                .creatorName(creator != null
                        ? (StringUtils.hasText(creator.getName()) ? creator.getName() : creator.getUsername())
                        : "未知用户")
                .knowledgeIds(knowledgeIds)
                .knowledgeNames(knowledgeNames)
                .createTime(question.getCreateTime())
                .updateTime(question.getUpdateTime())
                .build();
    }

    @Override
    public ImportResultVO importQuestions(MultipartFile file, Long courseId) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        // 如果外部没传 courseId，我们稍后尝试从 DTO 中取
        LoginUser loginUser = requireLoginUser();

        List<QuestionImportDTO> list;
        try {
            list = EasyExcel.read(file.getInputStream())
                    .head(QuestionImportDTO.class)
                    .sheet()
                    .doReadSync();
        } catch (Exception e) {
            throw new RuntimeException("文件解析失败，请确保上传的是标准的 Excel 或 CSV 文件");
        }

        if (list == null || list.isEmpty()) {
            return ImportResultVO.builder()
                    .successCount(0)
                    .failCount(1)
                    .errors(Collections.singletonList("未在文件中检测到有效数据行"))
                    .build();
        }

        int success = 0;
        int fail = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            QuestionImportDTO dto = list.get(i);
            // 简单空行过滤：如果内容和答案都为空，跳过
            if (dto == null || (!StringUtils.hasText(dto.getContent()) && !StringUtils.hasText(dto.getAnswer()))) {
                continue;
            }

            // 跳过表头行（如果 course_id 包含非数字字符）
            if (i == 0 && StringUtils.hasText(dto.getCourseId()) && !dto.getCourseId().trim().matches("\\d+")) {
                continue;
            }

            // 使用编程式事务处理单行导入，确保每一行都能即时落库
            try {
                // 1. 数据准备与校验
                Long finalCourseId = null;
                if (StringUtils.hasText(dto.getCourseId())) {
                    String courseStr = dto.getCourseId().trim();
                    if (courseStr.matches("\\d+")) {
                        finalCourseId = Long.valueOf(courseStr);
                    } else {
                        // 按课程名称查找
                        LambdaQueryWrapper<Course> courseQw = new LambdaQueryWrapper<>();
                        courseQw.eq(Course::getCourseName, courseStr);
                        List<Course> courses = courseMapper.selectList(courseQw);
                        if (!courses.isEmpty()) {
                            finalCourseId = courses.get(0).getId();
                        }
                    }
                }

                if (finalCourseId == null)
                    finalCourseId = courseId;
                if (finalCourseId == null)
                    throw new RuntimeException("缺少课程或课程ID格式错误");

                requireTeacherOfCourseOrAdmin(loginUser, finalCourseId);

                QuestionSaveDTO saveDTO = new QuestionSaveDTO();
                saveDTO.setCourseId(finalCourseId);

                if (!StringUtils.hasText(dto.getTypeId()))
                    throw new RuntimeException("题型ID/名称不能为空");

                String typeStr = dto.getTypeId().trim();
                Integer typeId = null;
                if (typeStr.matches("\\d+")) {
                    typeId = Integer.valueOf(typeStr);
                } else {
                    // 按名称查找
                    LambdaQueryWrapper<QuestionType> typeQw = new LambdaQueryWrapper<>();
                    typeQw.eq(QuestionType::getTypeName, typeStr);
                    QuestionType qt = questionTypeMapper.selectOne(typeQw);
                    if (qt != null) {
                        typeId = qt.getId();
                    }
                }
                if (typeId == null) {
                    throw new RuntimeException("题型 [" + typeStr + "] 不存在");
                }
                saveDTO.setTypeId(typeId);
                saveDTO.setContent(dto.getContent());

                if (StringUtils.hasText(dto.getDifficulty())) {
                    String diffStr = dto.getDifficulty().trim();
                    if (diffStr.matches("\\d+")) {
                        saveDTO.setDifficulty(Integer.valueOf(diffStr));
                    } else if (diffStr.contains("简单")) {
                        saveDTO.setDifficulty(1);
                    } else if (diffStr.contains("中等")) {
                        saveDTO.setDifficulty(2);
                    } else if (diffStr.contains("困难")) {
                        saveDTO.setDifficulty(3);
                    } else {
                        saveDTO.setDifficulty(1);
                    }
                } else {
                    saveDTO.setDifficulty(1);
                }

                saveDTO.setAnswer(dto.getAnswer());
                saveDTO.setAnalysis(dto.getAnalysis());

                if (StringUtils.hasText(dto.getOptions())) {
                    try {
                        String optStr = dto.getOptions().trim();
                        if (optStr.startsWith("[") && optStr.endsWith("]")) {
                            saveDTO.setOptions(objectMapper.readValue(optStr, new TypeReference<List<String>>() {
                            }));
                        } else {
                            saveDTO.setOptions(Arrays.asList(optStr.split("[,，]")));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("选项格式解析失败");
                    }
                }

                if (StringUtils.hasText(dto.getKnowledgeNames())) {
                    // 支持多个知识点，用分号或中文分号分隔
                    List<String> items = Arrays.stream(dto.getKnowledgeNames().split("[;；]"))
                            .map(String::trim)
                            .filter(StringUtils::hasText)
                            .collect(Collectors.toList());

                    if (!items.isEmpty()) {
                        List<Long> kIds = new ArrayList<>();
                        for (String item : items) {
                            // 支持多级知识点，用逗号分隔
                            List<String> path = Arrays.stream(item.split("[,，]"))
                                    .map(String::trim)
                                    .filter(StringUtils::hasText)
                                    .collect(Collectors.toList());

                            if (path.isEmpty())
                                continue;

                            if (path.size() == 1) {
                                // 单个知识点名称，需在课程下唯一
                                String name = path.get(0);
                                LambdaQueryWrapper<KnowledgePoint> kpQw = new LambdaQueryWrapper<>();
                                kpQw.eq(KnowledgePoint::getCourseId, finalCourseId);
                                kpQw.eq(KnowledgePoint::getName, name);
                                List<KnowledgePoint> kps = knowledgePointMapper.selectList(kpQw);
                                if (kps.isEmpty()) {
                                    throw new RuntimeException("知识点 [" + name + "] 不存在");
                                }
                                if (kps.size() > 1) {
                                    throw new RuntimeException("知识点 [" + name + "] 在课程下不唯一，请指定父级知识点（如：父级,子级）");
                                }
                                kIds.add(kps.get(0).getId());
                            } else {
                                // 多级知识点路径，按层级查找
                                Long currentParentId = 0L; // 假设顶级父ID为0
                                for (int j = 0; j < path.size(); j++) {
                                    String partName = path.get(j);
                                    LambdaQueryWrapper<KnowledgePoint> kpQw = new LambdaQueryWrapper<>();
                                    kpQw.eq(KnowledgePoint::getCourseId, finalCourseId);
                                    kpQw.eq(KnowledgePoint::getName, partName);
                                    if (currentParentId == 0L) {
                                        kpQw.and(w -> w.isNull(KnowledgePoint::getParentId).or()
                                                .eq(KnowledgePoint::getParentId, 0L));
                                    } else {
                                        kpQw.eq(KnowledgePoint::getParentId, currentParentId);
                                    }

                                    KnowledgePoint kp = knowledgePointMapper.selectOne(kpQw);
                                    if (kp == null) {
                                        throw new RuntimeException("多级知识点路径中 [" + partName + "] 不存在（所属父级ID: "
                                                + (currentParentId == 0L ? "无" : currentParentId) + "）");
                                    }
                                    currentParentId = kp.getId();
                                }
                                kIds.add(currentParentId);
                            }
                        }
                        saveDTO.setKnowledgeIds(kIds);
                    }
                }

                // 2. 插入数据库（这里调用 doCreateQuestion）
                doCreateQuestion(saveDTO, loginUser.getUser().getId());
                success++;
            } catch (Exception e) {
                fail++;
                String errorMsg = e.getMessage();
                if (e instanceof DuplicateKeyException)
                    errorMsg = "题目内容重复";
                errors.add("第 " + (i + 2) + " 行: " + errorMsg);
            }
        }

        // 清除缓存
        clearQuestionCache();
        return ImportResultVO.builder()
                .successCount(success)
                .failCount(fail > 0 || (success == 0 && list.size() > 0) ? Math.max(fail, 1) : 0)
                .errors(success == 0 && fail == 0 ? Collections.singletonList("所有行均被跳过，请检查第一列是否为正确的课程ID") : errors)
                .build();
    }

    /**
     * 内部保存方法，用于导入等场景
     */
    private void doCreateQuestion(QuestionSaveDTO saveDTO, Long creatorId) {
        validateSaveDTO(saveDTO);

        Question question = Question.builder()
                .courseId(saveDTO.getCourseId())
                .typeId(saveDTO.getTypeId())
                .content(saveDTO.getContent())
                .difficulty(saveDTO.getDifficulty())
                .options(toOptionsJson(saveDTO.getOptions()))
                .answer(saveDTO.getAnswer())
                .analysis(saveDTO.getAnalysis())
                .status(2) // 默认已发布
                .createBy(creatorId)
                .build();

        int rows = questionMapper.insert(question);
        if (rows != 1) {
            throw new RuntimeException("数据库写入失败");
        }

        saveKnowledgeMappings(question.getId(), saveDTO.getKnowledgeIds(), saveDTO.getCourseId());
    }

    @Override
    public void exportQuestions(QuestionQueryDTO queryDTO, HttpServletResponse response) {
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
                writeEmptyExcel(response);
                return;
            }
            qw.in(Question::getId, ids);
        }

        qw.orderByDesc(Question::getId);
        List<Question> list = questionMapper.selectList(qw);

        List<QuestionImportDTO> exportList = list.stream().map(q -> {
            QuestionImportDTO dto = new QuestionImportDTO();

            // 导出课程名称
            Course course = courseMapper.selectById(q.getCourseId());
            dto.setCourseId(course != null ? course.getCourseName()
                    : (q.getCourseId() != null ? String.valueOf(q.getCourseId()) : ""));

            // 导出题型名称
            QuestionType qt = questionTypeMapper.selectById(q.getTypeId());
            dto.setTypeId(qt != null ? qt.getTypeName() : (q.getTypeId() != null ? String.valueOf(q.getTypeId()) : ""));

            dto.setContent(q.getContent());

            // 导出难度名称
            String diffStr = "";
            if (q.getDifficulty() != null) {
                if (q.getDifficulty() == 1)
                    diffStr = "简单";
                else if (q.getDifficulty() == 2)
                    diffStr = "中等";
                else if (q.getDifficulty() == 3)
                    diffStr = "困难";
                else
                    diffStr = String.valueOf(q.getDifficulty());
            }
            dto.setDifficulty(diffStr);

            dto.setOptions(q.getOptions());
            dto.setAnswer(q.getAnswer());
            dto.setAnalysis(q.getAnalysis());

            List<Long> kIds = questionKnowledgeMapper.selectList(new LambdaQueryWrapper<QuestionKnowledge>()
                    .eq(QuestionKnowledge::getQuestionId, q.getId()))
                    .stream()
                    .map(QuestionKnowledge::getKnowledgePointId)
                    .toList();
            if (!kIds.isEmpty()) {
                List<String> pathStrings = new ArrayList<>();
                for (Long kId : kIds) {
                    pathStrings.add(getKnowledgeFullPath(kId));
                }
                dto.setKnowledgeNames(String.join(";", pathStrings));
            }
            return dto;
        }).toList();

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = java.net.URLEncoder.encode("题目列表", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), QuestionImportDTO.class).sheet("题目").doWrite(exportList);
        } catch (Exception e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage());
        }
    }

    private String getKnowledgeFullPath(Long id) {
        if (id == null)
            return "";
        List<String> path = new ArrayList<>();
        KnowledgePoint kp = knowledgePointMapper.selectById(id);
        while (kp != null) {
            path.add(0, kp.getName());
            if (kp.getParentId() == null || kp.getParentId() == 0L) {
                break;
            }
            kp = knowledgePointMapper.selectById(kp.getParentId());
        }
        return String.join(",", path);
    }

    private void writeEmptyExcel(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = java.net.URLEncoder.encode("题目列表", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), QuestionImportDTO.class).sheet("题目")
                    .doWrite(Collections.emptyList());
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditQuestion(Long questionId, QuestionAuditDTO auditDTO) {
        if (questionId == null) {
            throw new RuntimeException("questionId 不能为空");
        }
        if (auditDTO == null || auditDTO.getStatus() == null) {
            throw new RuntimeException("审核状态不能为空");
        }

        Question existing = questionMapper.selectById(questionId);
        if (existing == null) {
            throw new RuntimeException("题目不存在");
        }

        LoginUser loginUser = requireLoginUser();
        if (!isAdmin(loginUser)) {
            throw new RuntimeException("仅管理员有权审核题目");
        }

        Question update = new Question();
        update.setId(questionId);
        update.setStatus(auditDTO.getStatus());

        int rows = questionMapper.updateById(update);
        if (rows != 1) {
            throw new RuntimeException("审核失败");
        }
        // 清除缓存
        clearQuestionCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteQuestions(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            deleteQuestion(id);
        }
        // 清除缓存
        clearQuestionCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAuditQuestions(List<Long> ids, QuestionAuditDTO auditDTO) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            auditQuestion(id, auditDTO);
        }
        // 清除缓存
        clearQuestionCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateKnowledge(List<Long> ids, List<Long> knowledgeIds) {
        if (ids == null || ids.isEmpty() || knowledgeIds == null) {
            return;
        }

        LoginUser loginUser = requireLoginUser();

        for (Long id : ids) {
            Question q = questionMapper.selectById(id);
            if (q == null)
                continue;

            requireQuestionOwnerOrAdmin(loginUser, q);

            // 清除旧的
            questionKnowledgeMapper
                    .delete(new LambdaQueryWrapper<QuestionKnowledge>().eq(QuestionKnowledge::getQuestionId, id));
            // 保存新的
            saveKnowledgeMappings(id, knowledgeIds, q.getCourseId());
        }
        // 清除缓存
        clearQuestionCache();
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
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
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
            Integer c = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM paper_question WHERE question_id = ?",
                    Integer.class, questionId);
            if (c != null && c > 0) {
                throw new RuntimeException("题目已被试卷引用，禁止删除");
            }
        }
        if (existsTable("exam_paper_question")) {
            Integer c = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM exam_paper_question WHERE question_id = ?",
                    Integer.class, questionId);
            if (c != null && c > 0) {
                throw new RuntimeException("题目已被试卷引用，禁止删除");
            }
        }
    }

    private boolean existsTable(String tableName) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName);
        return c != null && c > 0;
    }

    /**
     * 清除试题分页列表缓存
     */
    private void clearQuestionCache() {
        Set<String> keys = redisUtils.keys("question:page:*");
        if (keys != null && !keys.isEmpty()) {
            redisUtils.delete(keys);
        }
    }
}
