package com.example.zaixiantiku.pojo.dto;

import lombok.Data;
import java.util.List;

@Data
public class PaperSaveDTO {
    private String paperName;
    private Long courseId;
    private Integer totalScore;
    private String remark;
    private List<PaperQuestionDTO> questions;
}
