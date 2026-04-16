package com.example.zaixiantiku.pojo.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class QuestionImportDTO {
    @ExcelProperty("课程ID")
    private String courseId;

    @ExcelProperty("题型ID")
    private String typeId;

    @ExcelProperty("题目内容")
    private String content;

    @ExcelProperty("难度")
    private String difficulty;

    @ExcelProperty("选项")
    private String options;

    @ExcelProperty("答案")
    private String answer;

    @ExcelProperty("解析")
    private String analysis;

    @ExcelProperty("知识点名称")
    private String knowledgeNames;
}
