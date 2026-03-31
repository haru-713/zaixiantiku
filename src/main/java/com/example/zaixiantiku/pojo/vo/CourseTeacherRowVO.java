package com.example.zaixiantiku.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseTeacherRowVO {

    private Long courseId;

    private Long id;

    private String name;
}
