package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.pojo.dto.QuestionQueryDTO;
import com.example.zaixiantiku.pojo.dto.QuestionSaveDTO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.QuestionDetailVO;
import com.example.zaixiantiku.pojo.vo.QuestionListVO;
import com.example.zaixiantiku.service.QuestionService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
@Tag(name = "题目管理", description = "题目创建/修改/删除/分页/详情")
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "创建题目")
    public Result<QuestionDetailVO> createQuestion(@RequestBody QuestionSaveDTO saveDTO) {
        QuestionDetailVO vo = questionService.createQuestion(saveDTO);
        return Result.success(vo);
    }

    @PutMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "修改题目")
    public Result<QuestionDetailVO> updateQuestion(@PathVariable Long questionId, @RequestBody QuestionSaveDTO saveDTO) {
        QuestionDetailVO vo = questionService.updateQuestion(questionId, saveDTO);
        return Result.success(vo);
    }

    @DeleteMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "删除题目")
    public Result<Void> deleteQuestion(@PathVariable Long questionId) {
        questionService.deleteQuestion(questionId);
        return Result.success(1, "删除成功", null);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "查询题目列表 (分页)")
    public Result<PageResult<QuestionListVO>> getQuestionPage(QuestionQueryDTO queryDTO) {
        PageResult<QuestionListVO> pageResult = questionService.getQuestionPage(queryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "获取题目详情")
    public Result<QuestionDetailVO> getQuestionDetail(@PathVariable Long questionId) {
        QuestionDetailVO vo = questionService.getQuestionDetail(questionId);
        return Result.success(vo);
    }
}

