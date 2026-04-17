package com.example.zaixiantiku.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 考试学生关联实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("exam_student")
public class ExamStudent {

    private Long examId;

    private Long studentId;
}
