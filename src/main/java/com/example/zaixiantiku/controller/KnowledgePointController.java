package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.pojo.dto.KnowledgePointSaveDTO;
import com.example.zaixiantiku.pojo.vo.KnowledgePointPageVO;
import com.example.zaixiantiku.pojo.vo.KnowledgePointSimpleVO;
import com.example.zaixiantiku.pojo.vo.KnowledgePointVO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.service.KnowledgePointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/knowledge-points")
@RequiredArgsConstructor
@Tag(name = "知识点管理", description = "知识点基础数据")
public class KnowledgePointController {

    private final KnowledgePointService knowledgePointService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @Operation(summary = "知识点列表", description = "根据课程查询知识点（用于选择）")
    public Result<List<KnowledgePointSimpleVO>> list(
            @RequestParam Long courseId,
            @RequestParam(required = false) String keyword) {
        return Result.success(knowledgePointService.listByCourse(courseId, keyword));
    }

    @GetMapping("/{kpId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @Operation(summary = "知识点详情")
    public Result<KnowledgePointVO> detail(@PathVariable Long kpId) {
        return Result.success(knowledgePointService.detail(kpId));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "知识点分页", description = "默认返回当前权限可见的知识点，可按课程/关键字筛选")
    public Result<PageResult<KnowledgePointPageVO>> page(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String keyword) {
        return Result.success(knowledgePointService.page(page, size, courseId, keyword));
    }

    @PostMapping("/{kpId}/move")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "知识点排序移动", description = "在同级节点内上移/下移")
    public Result<Boolean> move(@PathVariable Long kpId, @RequestParam String direction) {
        boolean moved = knowledgePointService.move(kpId, direction);
        boolean up = "UP".equalsIgnoreCase(direction) || "UPPER".equalsIgnoreCase(direction);
        boolean down = "DOWN".equalsIgnoreCase(direction) || "LOWER".equalsIgnoreCase(direction);
        if (!up && !down) {
            throw new RuntimeException("direction 参数非法");
        }
        String msg;
        if (moved) {
            msg = up ? "已上移" : "已下移";
        } else {
            msg = up ? "已是最顶部，无法上移" : "已是最底部，无法下移";
        }
        return Result.success(1, msg, moved);
    }

    @PutMapping("/{kpId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "修改知识点")
    public Result<KnowledgePointVO> update(@PathVariable Long kpId, @RequestBody KnowledgePointSaveDTO saveDTO) {
        KnowledgePointVO vo = knowledgePointService.update(kpId, saveDTO);
        return Result.success(vo);
    }

    @DeleteMapping("/{kpId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "删除知识点")
    public Result<Void> delete(@PathVariable Long kpId) {
        knowledgePointService.delete(kpId);
        return Result.success(1, "删除成功", null);
    }
}
