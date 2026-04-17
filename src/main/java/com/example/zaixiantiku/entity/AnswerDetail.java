package com.example.zaixiantiku.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 答题详情实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("answer_detail")
public class AnswerDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long examRecordId;
    private Long questionId;
    private String userAnswer;
    
    /**
     * 是否正确：0-错误，1-正确
     */
    private Integer isCorrect;
    
    private Integer score;
    
    private Integer timeSpent;
}
