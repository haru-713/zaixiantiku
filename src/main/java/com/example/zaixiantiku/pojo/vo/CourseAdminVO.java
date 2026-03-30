package com.example.zaixiantiku.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员课程列表项")
public class CourseAdminVO {

    private Long id;

    private String courseName;

    private String description;

    private String cover;

    private Integer status;

    private Integer auditStatus;

    private String auditReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Schema(description = "教师姓名（多个用逗号分隔）")
    private String teacherNames;
}
