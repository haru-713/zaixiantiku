package com.example.zaixiantiku.pojo.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class QuestionImportDTO {
    @ExcelProperty("所属课程")
    private String courseId;

    @ExcelProperty("题型")
    private String typeId;

    @ExcelProperty("题目内容")
    private String content;

    @ExcelProperty("难度(简单/中等/困难)")
    private String difficulty;

    @ExcelProperty("选项")
    private String options;

    @ExcelProperty("答案")
    private String answer;

    @ExcelProperty("解析")
    private String analysis;

    @ExcelProperty("知识点名称(多个用分号,层级用逗号)")
    private String knowledgeNames;
}
