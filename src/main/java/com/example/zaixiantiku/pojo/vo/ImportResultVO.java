package com.example.zaixiantiku.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "导入结果")
public class ImportResultVO {
    @Schema(description = "成功数量")
    private Integer successCount;
    @Schema(description = "失败数量")
    private Integer failCount;
    @Schema(description = "错误详情列表")
    private List<String> errors;
}
