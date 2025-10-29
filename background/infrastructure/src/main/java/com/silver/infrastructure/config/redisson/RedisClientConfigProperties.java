package com.silver.infrastructure.config.redisson;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 连接配置 <a href="https://github.com/redisson/redisson/tree/master/redisson-spring-boot-starter">redisson-spring-boot-starter</a>
 */
@ConfigurationProperties(prefix = "redis.sdk.config", ignoreInvalidFields = true)
public class RedisClientConfigProperties {

    /** host:ip */
    public String host;
    /** 端口 */
    public int port;
    /** 账密 */
    public String password;
    /** 设置连接池的大小，默认为64 */
    public int poolSize = 64;
    /** 设置连接池的最小空闲连接数，默认为10 */
    public int minIdleSize = 10;
    /** 设置连接的最大空闲时间（单位：毫秒），超过该时间的空闲连接将被关闭，默认为10000 */
    public int idleTimeout = 10000;
    /** 设置连接超时时间（单位：毫秒），默认为10000 */
    public int connectTimeout = 10000;
    /** 设置连接重试次数，默认为3 */
    public int retryAttempts = 3;
    /** 设置连接重试的间隔时间（单位：毫秒），默认为1000 */
    public int retryInterval = 1000;
    /** 设置定期检查连接是否可用的时间间隔（单位：毫秒），默认为0，表示不进行定期检查 */
    public int pingInterval = 0;
    /** 设置是否保持长连接，默认为true */
    public boolean keepAlive = true;


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getMinIdleSize() {
        return minIdleSize;
    }

    public void setMinIdleSize(int minIdleSize) {
        this.minIdleSize = minIdleSize;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }

    public int getPingInterval() {
        return pingInterval;
    }

    public void setPingInterval(int pingInterval) {
        this.pingInterval = pingInterval;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }
}
