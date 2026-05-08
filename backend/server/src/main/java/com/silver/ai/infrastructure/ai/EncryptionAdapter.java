package com.silver.ai.infrastructure.ai;

import com.silver.ai.domain.provider.port.EncryptionPort;
import com.silver.ai.shared.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 加密适配器 — 桥接领域层 EncryptionPort 到 CryptoUtil。
 */
@Component
public class EncryptionAdapter implements EncryptionPort {

    @Value("${app.crypto.secret-key}")
    private String secretKey;

    @Override
    public String encrypt(String plaintext) {
        return CryptoUtil.encrypt(plaintext, secretKey);
    }

    @Override
    public String decrypt(String ciphertext) {
        return CryptoUtil.decrypt(ciphertext, secretKey);
    }
}
