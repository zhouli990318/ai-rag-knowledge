package com.silver.ai.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GitImportRequest {
    @NotBlank(message = "仓库地址不能为空")
    @Size(max = 500, message = "仓库地址长度不能超过 500")
    @Pattern(regexp = "^https?://.*", message = "仓库地址必须以 http:// 或 https:// 开头")
    private String repoUrl;
    @Size(max = 100, message = "用户名长度不能超过 100")
    private String userName;
    @Size(max = 500, message = "token 长度不能超过 500")
    private String token;
}
