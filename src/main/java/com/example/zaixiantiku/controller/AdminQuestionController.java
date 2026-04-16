package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.pojo.dto.QuestionAuditDTO;
import com.example.zaixiantiku.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/questions")
@RequiredArgsConstructor
@Tag(name = "题目审核 (管理员)", description = "管理员对题目进行发布或禁用")
public class AdminQuestionController {

    private final QuestionService questionService;

    @PutMapping("/{questionId}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "题目审核")
    public Result<Void> auditQuestion(@PathVariable Long questionId, @RequestBody QuestionAuditDTO auditDTO) {
        questionService.auditQuestion(questionId, auditDTO);
        return Result.success(1, "审核完成", null);
    }
}
