package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.pojo.dto.QuestionQueryDTO;
import com.example.zaixiantiku.pojo.dto.QuestionSaveDTO;
import com.example.zaixiantiku.pojo.vo.ImportResultVO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.QuestionDetailVO;
import com.example.zaixiantiku.pojo.vo.QuestionListVO;
import com.example.zaixiantiku.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
@Tag(name = "试题管理", description = "试题创建/修改/删除/分页/详情/导入/导出")
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "创建试题")
    public Result<QuestionDetailVO> createQuestion(@RequestBody QuestionSaveDTO saveDTO) {
        QuestionDetailVO vo = questionService.createQuestion(saveDTO);
        return Result.success(vo);
    }

    @PutMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "修改试题")
    public Result<QuestionDetailVO> updateQuestion(@PathVariable Long questionId,
            @RequestBody QuestionSaveDTO saveDTO) {
        QuestionDetailVO vo = questionService.updateQuestion(questionId, saveDTO);
        return Result.success(vo);
    }

    @DeleteMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "删除试题")
    public Result<Void> deleteQuestion(@PathVariable Long questionId) {
        questionService.deleteQuestion(questionId);
        return Result.success(1, "删除成功", null);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "查询试题列表 (分页)")
    public Result<PageResult<QuestionListVO>> getQuestionPage(QuestionQueryDTO queryDTO) {
        PageResult<QuestionListVO> pageResult = questionService.getQuestionPage(queryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "获取试题详情")
    public Result<QuestionDetailVO> getQuestionDetail(@PathVariable Long questionId) {
        QuestionDetailVO vo = questionService.getQuestionDetail(questionId);
        return Result.success(vo);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "批量导入试题")
    public Result<ImportResultVO> importQuestions(@RequestParam("file") MultipartFile file,
            @RequestParam("courseId") Long courseId) {
        ImportResultVO res = questionService.importQuestions(file, courseId);
        String msg = String.format("导入成功，共导入%d道题", res.getSuccessCount());
        return Result.success(1, msg, res);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "批量导出试题")
    public void exportQuestions(QuestionQueryDTO queryDTO, HttpServletResponse response) {
        questionService.exportQuestions(queryDTO, response);
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "批量删除试题")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        questionService.batchDeleteQuestions(ids);
        return Result.success(1, "批量删除成功", null);
    }

    @PutMapping("/batch/knowledge")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "批量修改试题知识点")
    public Result<Void> batchUpdateKnowledge(@RequestParam("ids") List<Long> ids,
            @RequestBody List<Long> knowledgeIds) {
        questionService.batchUpdateKnowledge(ids, knowledgeIds);
        return Result.success(1, "批量修改知识点成功", null);
    }
}
