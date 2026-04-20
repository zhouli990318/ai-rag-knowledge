package com.silver.ai.mcpgateway.infrastructure.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "redis.sdk.config")
public class RedisConfig {

    private String host = "localhost";
    private int port = 6379;
    private String password;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;
        config.useSingleServer()
                .setAddress(address)
                .setConnectionMinimumIdleSize(5)
                .setConnectionPoolSize(20);
        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }
        return Redisson.create(config);
    }
}