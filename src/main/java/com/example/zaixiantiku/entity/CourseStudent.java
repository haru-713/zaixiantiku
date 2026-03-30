package com.example.zaixiantiku.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("course_student")
public class CourseStudent {

    private Long courseId;

    private Long studentId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinTime;
}
