package com.silver.ai.interfaces.dto;

import lombok.Data;

@Data
public class SearchRequest {
    private String query;
    private int topK = 5;
}
