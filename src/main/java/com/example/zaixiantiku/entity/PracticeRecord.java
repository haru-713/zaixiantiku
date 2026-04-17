package com.example.zaixiantiku.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "practice_record", autoResultMap = true)
public class PracticeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long courseId;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Object> questionIds;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> answers;
    
    private Integer totalScore;
    private Integer totalDuration;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
}
