package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.pojo.dto.KnowledgePointSaveDTO;
import com.example.zaixiantiku.pojo.vo.KnowledgePointTreeVO;
import com.example.zaixiantiku.pojo.vo.KnowledgePointVO;
import com.example.zaixiantiku.service.KnowledgePointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/courses/{courseId}/knowledge-points")
@RequiredArgsConstructor
@Tag(name = "知识点管理(课程)", description = "按课程创建/树形查询知识点")
public class CourseKnowledgePointController {

    private final KnowledgePointService knowledgePointService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "创建知识点")
    public Result<KnowledgePointVO> create(@PathVariable Long courseId, @RequestBody KnowledgePointSaveDTO saveDTO) {
        KnowledgePointVO vo = knowledgePointService.create(courseId, saveDTO);
        return Result.success(vo);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @Operation(summary = "查询知识点(树形)")
    public Result<List<KnowledgePointTreeVO>> tree(@PathVariable Long courseId) {
        return Result.success(knowledgePointService.tree(courseId));
    }
}

