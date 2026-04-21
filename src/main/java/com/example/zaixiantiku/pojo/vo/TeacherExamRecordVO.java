package com.example.zaixiantiku.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 教师端考试记录 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherExamRecordVO {
    private Long id;
    private Long examId;
    private String examName;
    private String paperName;
    private String studentName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime submitTime;

    private Integer totalScore;
    private Integer status; // 0-考试中, 1-已交卷(待阅卷), 2-已阅卷
    
    private Integer cheatCount;
    private Boolean forceSubmit;

    private String courseTeacherNames; // 该课程下的所有任课教师（阅卷人）
    private Integer totalExamCount; // 该场考试的总交卷人数
    private Integer markedExamCount; // 该场考试的已阅卷人数
}
