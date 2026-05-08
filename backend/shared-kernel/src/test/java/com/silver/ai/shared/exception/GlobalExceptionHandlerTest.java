package com.silver.ai.shared.exception;

import com.silver.ai.shared.result.ApiResponse;
import com.silver.ai.shared.result.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.PayloadTooLargeException;
import org.springframework.web.server.ServerWebInputException;

import java.lang.reflect.Method;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessExceptionShouldReturnBadRequest() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(
                new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND, "99")
        );
        ApiResponse<Void> body = assertBody(response);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.CONVERSATION_NOT_FOUND.getCode(), body.getCode());
        assertEquals(ErrorCode.CONVERSATION_NOT_FOUND.getMessage() + ": 99", body.getMessage());
    }

    @Test
    void handleValidationShouldAggregateFieldErrors() throws Exception {
        SampleRequest request = new SampleRequest();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "request");
        bindingResult.addError(new FieldError("request", "name", "不能为空"));

        Method method = Objects.requireNonNull(
            GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleEndpoint", SampleRequest.class)
        );
        MethodParameter parameter = new MethodParameter(method, 0);
        WebExchangeBindException exception = new WebExchangeBindException(parameter, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(exception);
        ApiResponse<Void> body = assertBody(response);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.INVALID_PARAMETER.getCode(), body.getCode());
        assertTrue(body.getMessage().contains("name: 不能为空"));
    }

    @Test
    void handleServerWebInputShouldIncludeReason() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleServerWebInput(
                new ServerWebInputException("Missing parameter: providerId")
        );
        ApiResponse<Void> body = assertBody(response);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.INVALID_PARAMETER.getCode(), body.getCode());
    }

    @Test
    void handleDataBufferLimitShouldReturnPayloadTooLarge() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleDataBufferLimit(
                new DataBufferLimitException("Exceeded limit"));
        ApiResponse<Void> body = assertBody(response);

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals(40013, body.getCode());
        assertEquals("文件大小超过限制", body.getMessage());
    }

    @Test
    void handlePayloadTooLargeShouldReturnPayloadTooLarge() {
        ResponseEntity<ApiResponse<Void>> response = handler.handlePayloadTooLarge(
                new PayloadTooLargeException(new RuntimeException("Request body too large")));
        ApiResponse<Void> body = assertBody(response);

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals(40013, body.getCode());
        assertEquals("文件大小超过限制", body.getMessage());
    }

    @Test
    void handleUnexpectedShouldReturnInternalError() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpected(new RuntimeException("boom"));
        ApiResponse<Void> body = assertBody(response);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), body.getCode());
        assertEquals(ErrorCode.INTERNAL_ERROR.getMessage(), body.getMessage());
    }

    private ApiResponse<Void> assertBody(ResponseEntity<ApiResponse<Void>> response) {
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        return body;
    }

    @SuppressWarnings("unused")
    private void sampleEndpoint(SampleRequest request) {
    }

    private static class SampleRequest {
        @SuppressWarnings("unused")
        private String name;
    }
}