package com.example.zaixiantiku.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 考试班级关联实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("exam_class")
public class ExamClass {

    private Long examId;

    private Long classId;
}
