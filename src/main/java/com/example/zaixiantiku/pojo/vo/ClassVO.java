package com.example.zaixiantiku.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 班级视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassVO {
    private Long id;
    private String className;
    private String grade;
    private Long teacherId;
    private LocalDateTime createTime;
}
