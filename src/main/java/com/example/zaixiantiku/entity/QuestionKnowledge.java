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
@TableName("question_knowledge")
public class QuestionKnowledge {

    private Long questionId;

    private Long knowledgePointId;
}

