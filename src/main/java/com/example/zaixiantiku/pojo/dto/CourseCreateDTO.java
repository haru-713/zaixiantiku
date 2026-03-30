package com.example.zaixiantiku.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建课程请求体")
public class CourseCreateDTO {

    @Schema(description = "课程名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String courseName;

    @Schema(description = "课程描述")
    private String description;

    @Schema(description = "封面URL")
    private String cover;
}
