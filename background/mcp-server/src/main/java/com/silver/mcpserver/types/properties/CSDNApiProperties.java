package com.silver.mcpserver.types.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "csdn.api")
@Component
@Data
public class CSDNApiProperties {

    private String cookie;

    private String categories;
}
