package com.example.zaixiantiku.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("paper_question")
public class PaperQuestion {

    private Long paperId;

    private Long questionId;

    private Integer score;

    private Integer sortOrder;
}
