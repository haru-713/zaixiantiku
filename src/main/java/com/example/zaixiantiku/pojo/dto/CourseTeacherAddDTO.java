package com.example.zaixiantiku.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "课程添加教师请求体")
public class CourseTeacherAddDTO {

    @Schema(description = "教师ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> teacherIds;
}

