package com.silver.ai.shared.exception;

import com.silver.ai.shared.result.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BusinessExceptionTest {

    @Test
    void constructorWithDetailShouldExposeCodeAndMessage() {
        BusinessException exception = new BusinessException(ErrorCode.PROVIDER_NOT_FOUND, "provider-1");

        assertEquals(ErrorCode.PROVIDER_NOT_FOUND, exception.getErrorCode());
        assertEquals(ErrorCode.PROVIDER_NOT_FOUND.getCode(), exception.getCode());
        assertEquals(ErrorCode.PROVIDER_NOT_FOUND.getMessage() + ": provider-1", exception.getMessage());
    }

    @Test
    void constructorWithCauseShouldRetainCause() {
        IllegalStateException cause = new IllegalStateException("boom");

        BusinessException exception = new BusinessException(ErrorCode.INTERNAL_ERROR, "detail", cause);

        assertSame(cause, exception.getCause());
        assertEquals(ErrorCode.INTERNAL_ERROR.getMessage() + ": detail", exception.getMessage());
    }
}