package com.example.zaixiantiku.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "课程列表项")
public class CourseListVO {

    private Long id;

    private String courseName;

    private String description;

    private String cover;

    private Integer status;

    private List<TeacherSimpleVO> teachers;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
