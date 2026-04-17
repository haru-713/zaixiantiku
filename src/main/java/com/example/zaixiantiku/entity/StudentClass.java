package com.example.zaixiantiku.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学生班级关联实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("student_class")
public class StudentClass {

    private Long studentId;

    private Long classId;

    private LocalDateTime joinTime;
}
