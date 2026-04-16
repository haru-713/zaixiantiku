package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.zaixiantiku.entity.Course;
import com.example.zaixiantiku.entity.CourseTeacher;
import com.example.zaixiantiku.entity.KnowledgePoint;
import com.example.zaixiantiku.mapper.CourseMapper;
import com.example.zaixiantiku.mapper.CourseTeacherMapper;
import com.example.zaixiantiku.mapper.KnowledgePointMapper;
import com.example.zaixiantiku.pojo.dto.KnowledgePointSaveDTO;
import com.example.zaixiantiku.pojo.vo.KnowledgePointPageVO;
import com.example.zaixiantiku.pojo.vo.KnowledgePointSimpleVO;
import com.example.zaixiantiku.pojo.vo.KnowledgePointTreeVO;
import com.example.zaixiantiku.pojo.vo.KnowledgePointVO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.security.LoginUser;
import com.example.zaixiantiku.service.KnowledgePointService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgePointServiceImpl implements KnowledgePointService {

    private final KnowledgePointMapper knowledgePointMapper;
    private final CourseMapper courseMapper;
    private final CourseTeacherMapper courseTeacherMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<KnowledgePointSimpleVO> listByCourse(Long courseId, String keyword) {
        if (courseId == null) {
            throw new RuntimeException("courseId 不能为空");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        LoginUser loginUser = requireLoginUser();
        requireTeacherOfCourseOrAdmin(loginUser, courseId);
        LambdaQueryWrapper<KnowledgePoint> qw = new LambdaQueryWrapper<>();
        qw.eq(KnowledgePoint::getCourseId, courseId);
        if (StringUtils.hasText(keyword)) {
            qw.like(KnowledgePoint::getName, keyword);
        }
        qw.orderByAsc(KnowledgePoint::getSortOrder).orderByAsc(KnowledgePoint::getId);
        List<KnowledgePoint> list = knowledgePointMapper.selectList(qw);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(kp -> KnowledgePointSimpleVO.builder()
                        .id(kp.getId())
                        .name(kp.getName())
                        .parentId(kp.getParentId())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgePointVO create(Long courseId, KnowledgePointSaveDTO saveDTO) {
        if (courseId == null) {
            throw new RuntimeException("courseId 不能为空");
        }
        if (saveDTO == null || !StringUtils.hasText(saveDTO.getName())) {
            throw new RuntimeException("name 不能为空");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        LoginUser loginUser = requireLoginUser();
        requireTeacherOfCourseOrAdmin(loginUser, courseId);

        Long parentId = saveDTO.getParentId();
        if (parentId != null) {
            KnowledgePoint parent = knowledgePointMapper.selectById(parentId);
            if (parent == null) {
                throw new RuntimeException("父知识点不存在");
            }
            if (!Objects.equals(parent.getCourseId(), courseId)) {
                throw new RuntimeException("父知识点与课程不匹配");
            }
        }

        Integer sortOrder = saveDTO.getSortOrder();
        if (sortOrder == null) {
            LambdaQueryWrapper<KnowledgePoint> maxQw = new LambdaQueryWrapper<>();
            maxQw.eq(KnowledgePoint::getCourseId, courseId);
            if (parentId == null) {
                maxQw.isNull(KnowledgePoint::getParentId);
            } else {
                maxQw.eq(KnowledgePoint::getParentId, parentId);
            }
            maxQw.orderByDesc(KnowledgePoint::getSortOrder)
                    .orderByDesc(KnowledgePoint::getId)
                    .last("LIMIT 1");

            KnowledgePoint max = knowledgePointMapper.selectOne(maxQw);
            int maxSort = max == null || max.getSortOrder() == null ? 0 : max.getSortOrder();
            sortOrder = maxSort + 1;
        } else if (sortOrder <= 0) {
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<KnowledgePoint> shift = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            shift.eq(KnowledgePoint::getCourseId, courseId);
            if (parentId == null) {
                shift.isNull(KnowledgePoint::getParentId);
            } else {
                shift.eq(KnowledgePoint::getParentId, parentId);
            }
            shift.setSql("sort_order = IFNULL(sort_order,0) + 1");
            knowledgePointMapper.update(null, shift);
            sortOrder = 1;
        }

        KnowledgePoint kp = KnowledgePoint.builder()
                .courseId(courseId)
                .name(saveDTO.getName().trim())
                .parentId(parentId)
                .sortOrder(sortOrder)
                .build();

        int rows = knowledgePointMapper.insert(kp);
        if (rows != 1 || kp.getId() == null) {
            throw new RuntimeException("创建失败");
        }
        KnowledgePoint created = knowledgePointMapper.selectById(kp.getId());
        return toVO(created);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgePointVO update(Long kpId, KnowledgePointSaveDTO saveDTO) {
        if (kpId == null) {
            throw new RuntimeException("kpId 不能为空");
        }
        if (saveDTO == null || !StringUtils.hasText(saveDTO.getName())) {
            throw new RuntimeException("name 不能为空");
        }

        KnowledgePoint existing = knowledgePointMapper.selectById(kpId);
        if (existing == null) {
            throw new RuntimeException("知识点不存在");
        }

        LoginUser loginUser = requireLoginUser();
        requireTeacherOfCourseOrAdmin(loginUser, existing.getCourseId());

        Long parentId = saveDTO.getParentId();
        if (parentId != null) {
            if (parentId.equals(kpId)) {
                throw new RuntimeException("parentId 不能为自身");
            }
            KnowledgePoint parent = knowledgePointMapper.selectById(parentId);
            if (parent == null) {
                throw new RuntimeException("父知识点不存在");
            }
            if (!Objects.equals(parent.getCourseId(), existing.getCourseId())) {
                throw new RuntimeException("父知识点与课程不匹配");
            }
        }

        KnowledgePoint update = new KnowledgePoint();
        update.setId(kpId);
        update.setName(saveDTO.getName().trim());
        update.setParentId(parentId);
        update.setSortOrder(saveDTO.getSortOrder() == null ? existing.getSortOrder() : saveDTO.getSortOrder());

        int rows = knowledgePointMapper.updateById(update);
        if (rows != 1) {
            throw new RuntimeException("修改失败");
        }
        KnowledgePoint updated = knowledgePointMapper.selectById(kpId);
        return toVO(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean move(Long kpId, String direction) {
        if (kpId == null) {
            throw new RuntimeException("kpId 不能为空");
        }
        if (!StringUtils.hasText(direction)) {
            throw new RuntimeException("direction 不能为空");
        }

        KnowledgePoint current = knowledgePointMapper.selectById(kpId);
        if (current == null) {
            throw new RuntimeException("知识点不存在");
        }

        LoginUser loginUser = requireLoginUser();
        requireTeacherOfCourseOrAdmin(loginUser, current.getCourseId());

        LambdaQueryWrapper<KnowledgePoint> siblingQw = new LambdaQueryWrapper<>();
        siblingQw.eq(KnowledgePoint::getCourseId, current.getCourseId());
        if (current.getParentId() == null) {
            siblingQw.isNull(KnowledgePoint::getParentId);
        } else {
            siblingQw.eq(KnowledgePoint::getParentId, current.getParentId());
        }
        siblingQw.orderByAsc(KnowledgePoint::getSortOrder).orderByAsc(KnowledgePoint::getId);

        List<KnowledgePoint> siblings = knowledgePointMapper.selectList(siblingQw);
        if (siblings == null || siblings.size() <= 1) {
            return false;
        }

        int idx = -1;
        for (int i = 0; i < siblings.size(); i++) {
            if (Objects.equals(siblings.get(i).getId(), kpId)) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return false;
        }

        boolean up = "UP".equalsIgnoreCase(direction) || "UPPER".equalsIgnoreCase(direction);
        boolean down = "DOWN".equalsIgnoreCase(direction) || "LOWER".equalsIgnoreCase(direction);
        if (!up && !down) {
            throw new RuntimeException("direction 参数非法");
        }

        int otherIdx = up ? idx - 1 : idx + 1;
        if (otherIdx < 0 || otherIdx >= siblings.size()) {
            return false;
        }

        java.util.Collections.swap(siblings, idx, otherIdx);

        for (int i = 0; i < siblings.size(); i++) {
            KnowledgePoint kp = siblings.get(i);
            int desired = i + 1;
            if (kp.getSortOrder() == null || !kp.getSortOrder().equals(desired)) {
                knowledgePointMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<KnowledgePoint>()
                                .eq(KnowledgePoint::getId, kp.getId())
                                .set(KnowledgePoint::getSortOrder, desired));
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long kpId) {
        if (kpId == null) {
            throw new RuntimeException("kpId 不能为空");
        }

        KnowledgePoint existing = knowledgePointMapper.selectById(kpId);
        if (existing == null) {
            throw new RuntimeException("知识点不存在");
        }

        LoginUser loginUser = requireLoginUser();
        requireTeacherOfCourseOrAdmin(loginUser, existing.getCourseId());

        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM question_knowledge WHERE knowledge_point_id = ?",
                Integer.class,
                kpId);
        if (c != null && c > 0) {
            throw new RuntimeException("知识点已被题目引用，禁止删除");
        }

        // 检查是否有子知识点
        Integer childCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM knowledge_point WHERE parent_id = ?",
                Integer.class,
                kpId);
        if (childCount != null && childCount > 0) {
            throw new RuntimeException("该知识点下有关联的子知识点，无法删除");
        }

        int rows = knowledgePointMapper.deleteById(kpId);
        if (rows != 1) {
            throw new RuntimeException("删除失败");
        }
    }

    @Override
    public List<KnowledgePointTreeVO> tree(Long courseId) {
        if (courseId == null) {
            throw new RuntimeException("courseId 不能为空");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        LoginUser loginUser = requireLoginUser();
        requireTeacherOfCourseOrAdmin(loginUser, courseId);

        List<KnowledgePoint> list = knowledgePointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getCourseId, courseId)
                .orderByAsc(KnowledgePoint::getSortOrder)
                .orderByAsc(KnowledgePoint::getId));

        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, KnowledgePointTreeVO> map = new HashMap<>();
        for (KnowledgePoint kp : list) {
            map.put(kp.getId(), KnowledgePointTreeVO.builder()
                    .id(kp.getId())
                    .name(kp.getName())
                    .parentId(kp.getParentId())
                    .sortOrder(kp.getSortOrder())
                    .children(new ArrayList<>())
                    .build());
        }

        List<KnowledgePointTreeVO> roots = new ArrayList<>();
        for (KnowledgePoint kp : list) {
            KnowledgePointTreeVO node = map.get(kp.getId());
            Long pid = kp.getParentId();
            if (pid == null || !map.containsKey(pid)) {
                roots.add(node);
            } else {
                map.get(pid).getChildren().add(node);
            }
        }

        return roots;
    }

    @Override
    public KnowledgePointVO detail(Long kpId) {
        if (kpId == null) {
            throw new RuntimeException("kpId 不能为空");
        }
        KnowledgePoint kp = knowledgePointMapper.selectById(kpId);
        if (kp == null) {
            throw new RuntimeException("知识点不存在");
        }
        LoginUser loginUser = requireLoginUser();
        requireTeacherOfCourseOrAdmin(loginUser, kp.getCourseId());
        return toVO(kp);
    }

    @Override
    public PageResult<KnowledgePointPageVO> page(Integer page, Integer size, Long courseId, String keyword) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : size;
        PageHelper.startPage(p, s);

        LoginUser loginUser = requireLoginUser();
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("SELECT kp.id, kp.course_id AS courseId, c.course_name AS courseName, ")
                .append("kp.name, kp.parent_id AS parentId, kp.sort_order AS sortOrder, kp.create_time AS createTime ")
                .append("FROM knowledge_point kp ")
                .append("JOIN course c ON c.id = kp.course_id ");

        if (!isAdmin(loginUser)) {
            sql.append("JOIN course_teacher ct ON ct.course_id = kp.course_id AND ct.teacher_id = ? ");
            params.add(loginUser.getUser().getId());
        }

        sql.append("WHERE 1=1 ");

        if (courseId != null) {
            sql.append("AND kp.course_id = ? ");
            params.add(courseId);
        }
        if (StringUtils.hasText(keyword)) {
            sql.append("AND kp.name LIKE ? ");
            params.add("%" + keyword.trim() + "%");
        }

        sql.append("ORDER BY kp.course_id ASC, kp.sort_order ASC, kp.id ASC ");

        List<KnowledgePointPageVO> list = jdbcTemplate.query(sql.toString(), params.toArray(),
                (rs, rowNum) -> KnowledgePointPageVO.builder()
                        .id(rs.getLong("id"))
                        .courseId(rs.getLong("courseId"))
                        .courseName(rs.getString("courseName"))
                        .name(rs.getString("name"))
                        .parentId(rs.getObject("parentId") == null ? null : rs.getLong("parentId"))
                        .sortOrder(rs.getObject("sortOrder") == null ? 0 : rs.getInt("sortOrder"))
                        .createTime(rs.getTimestamp("createTime").toLocalDateTime())
                        .build());

        PageInfo<KnowledgePointPageVO> pageInfo = new PageInfo<>(list);
        return PageResult.of(pageInfo.getTotal(), list);
    }

    private KnowledgePointVO toVO(KnowledgePoint kp) {
        if (kp == null) {
            return null;
        }
        return KnowledgePointVO.builder()
                .id(kp.getId())
                .courseId(kp.getCourseId())
                .name(kp.getName())
                .parentId(kp.getParentId())
                .sortOrder(kp.getSortOrder())
                .createTime(kp.getCreateTime())
                .build();
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
}
