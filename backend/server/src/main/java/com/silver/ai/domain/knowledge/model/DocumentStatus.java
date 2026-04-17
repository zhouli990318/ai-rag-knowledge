package com.silver.ai.domain.knowledge.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocumentStatus {
    PENDING("待处理"),
    PROCESSING("处理中"),
    INDEXED("已索引"),
    FAILED("处理失败");

    private final String description;
}
