package com.example.zaixiantiku.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "题目列表项")
public class QuestionListVO {

    private Long id;

    private String content;

    private String courseName;

    private String creatorName;

    private Integer typeId;

    private Integer difficulty;

    private Integer status;

    private java.util.List<String> knowledgeNames;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

