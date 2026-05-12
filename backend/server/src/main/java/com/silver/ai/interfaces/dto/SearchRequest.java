package com.silver.ai.interfaces.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SearchRequest {
    @NotBlank(message = "查询内容不能为空")
    @Size(max = 2000, message = "查询内容长度不能超过 2000")
    private String query;
    @Min(value = 1, message = "topK 最小为 1")
    @Max(value = 100, message = "topK 最大为 100")
    private int topK = 5;
    @Size(max = 500, message = "filterExpression 长度不能超过 500")
    private String filterExpression;
}
