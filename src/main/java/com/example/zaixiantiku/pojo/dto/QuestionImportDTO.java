package com.example.zaixiantiku.pojo.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class QuestionImportDTO {
    @ExcelProperty("题型ID")
    private Integer typeId;

    @ExcelProperty("题目内容")
    private String content;

    @ExcelProperty("难度(1-简单,2-中等,3-困难)")
    private Integer difficulty;

    @ExcelProperty("选项(JSON格式)")
    private String options;

    @ExcelProperty("标准答案")
    private String answer;

    @ExcelProperty("解析")
    private String analysis;

    @ExcelProperty("知识点名称(多个逗号隔开)")
    private String knowledgeNames;
}
