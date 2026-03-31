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
@Schema(description = "课程详情")
public class CourseDetailVO {

    private Long id;

    private String courseName;

    private String description;

    private String cover;

    private Integer status;

    private Integer auditStatus;

    private String auditReason;

    private List<TeacherSimpleVO> teachers;

    private List<StudentSimpleVO> students;

    @Schema(description = "是否允许添加教师（仅管理员）")
    private Boolean canAddTeacher;

    @Schema(description = "是否允许移除教师（管理员、课程创建者）")
    private Boolean canRemoveTeacher;

    @Schema(description = "是否允许添加学生（课程教师、管理员）")
    private Boolean canAddStudent;

    @Schema(description = "是否允许移除学生（课程教师、管理员）")
    private Boolean canRemoveStudent;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
