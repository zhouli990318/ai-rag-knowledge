package com.silver.ai.domain.provider.port;

/**
 * 加密端口 — 领域层定义，基础设施层实现。
 * 用于 API Key 等敏感信息的加密处理。
 */
public interface EncryptionPort {

    String encrypt(String plaintext);

    String decrypt(String ciphertext);
}
