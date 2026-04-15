package com.example.zaixiantiku.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("question_type")
public class QuestionType {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String typeName;

    private String typeCode;

    private Integer status;

    private Integer sortOrder;
}

