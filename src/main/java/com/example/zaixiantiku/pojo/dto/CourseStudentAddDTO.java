package com.example.zaixiantiku.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "课程添加学生请求体")
public class CourseStudentAddDTO {

    @Schema(description = "学生ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> studentIds;
}
