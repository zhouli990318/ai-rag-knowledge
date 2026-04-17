package com.silver.ai.shared.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CryptoUtilTest {

    @Test
    void encryptAndDecryptShouldRoundTrip() {
        String plaintext = "secret-api-key";
        String key = "test-key-123";

        String ciphertext = CryptoUtil.encrypt(plaintext, key);

        assertNotEquals(plaintext, ciphertext);
        assertEquals(plaintext, CryptoUtil.decrypt(ciphertext, key));
    }

    @Test
    void encryptShouldProduceDifferentCiphertextForSamePlaintext() {
        String plaintext = "same-text";
        String key = "repeatable-key";

        String encrypted1 = CryptoUtil.encrypt(plaintext, key);
        String encrypted2 = CryptoUtil.encrypt(plaintext, key);

        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void decryptShouldFailWhenKeyDoesNotMatch() {
        String ciphertext = CryptoUtil.encrypt("hello", "right-key");

        assertThrows(IllegalStateException.class, () -> CryptoUtil.decrypt(ciphertext, "wrong-key"));
    }
}