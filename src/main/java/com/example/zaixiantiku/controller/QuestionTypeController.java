package com.example.zaixiantiku.controller;

import com.example.zaixiantiku.common.Result;
import com.example.zaixiantiku.pojo.vo.QuestionTypeVO;
import com.example.zaixiantiku.service.QuestionTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/question-types")
@RequiredArgsConstructor
@Tag(name = "题型管理", description = "题型基础数据")
public class QuestionTypeController {

    private final QuestionTypeService questionTypeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @Operation(summary = "题型列表", description = "返回启用状态的题型列表（用于下拉选择）")
    public Result<List<QuestionTypeVO>> listTypes() {
        return Result.success(questionTypeService.listEnabledTypes());
    }
}

