package com.silver.mcpserver.infrastructure.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WeChatTokenResponseDTO {

    @JsonProperty("access_token")
    private String token;

    @JsonProperty("expires_in")
    private Long expireIn;

    private LocalDateTime expireAt;

    public WeChatTokenResponseDTO() {
    }

    public WeChatTokenResponseDTO(String token, Long expireIn) {
        this.token = token;
        this.expireIn = expireIn;
        this.expireAt = LocalDateTime.now().plusSeconds(expireIn);
    }

    public void setExpireIn(Long expireIn) {
        this.expireIn = expireIn;
        this.expireAt = LocalDateTime.now().plusSeconds(expireIn);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireAt);
    }
}
