package com.silver.mcpserver.types.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "wechat.api")
@Component
@Data
public class WeChatApiProperties {
    private String originalId;
    private String appId;
    private String appSecret;
    private String templateId;
    private String toUser;
}
