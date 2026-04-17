package com.silver.ai.shared.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiResponseTest {

    @Test
    void okShouldBuildSuccessResponseWithData() {
        ApiResponse<String> response = ApiResponse.ok("payload");

        assertEquals(200, response.getCode());
        assertEquals("success", response.getMessage());
        assertEquals("payload", response.getData());
        assertTrue(response.getTimestamp() > 0);
    }

    @Test
    void okWithoutDataShouldBuildEmptySuccessResponse() {
        ApiResponse<Void> response = ApiResponse.ok();

        assertEquals(200, response.getCode());
        assertEquals("success", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void failShouldBuildResponseFromErrorCodeAndDetail() {
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.CONVERSATION_NOT_FOUND, "42");

        assertEquals(ErrorCode.CONVERSATION_NOT_FOUND.getCode(), response.getCode());
        assertEquals(ErrorCode.CONVERSATION_NOT_FOUND.getMessage() + ": 42", response.getMessage());
        assertNull(response.getData());
    }
}