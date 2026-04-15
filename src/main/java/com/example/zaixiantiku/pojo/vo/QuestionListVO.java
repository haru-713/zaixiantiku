package com.example.zaixiantiku.pojo.vo;

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

    private Integer typeId;

    private Integer difficulty;

    private Integer status;

    private LocalDateTime createTime;
}

