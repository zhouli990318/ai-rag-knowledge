package com.silver.ai.interfaces.dto;

import lombok.Data;

@Data
public class GitImportRequest {
    private String repoUrl;
    private String userName;
    private String token;
}
