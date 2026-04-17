package com.silver.ai.shared.exception;

import com.silver.ai.shared.result.ApiResponse;
import com.silver.ai.shared.result.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessExceptionShouldReturnBadRequest() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(
                new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND, "99")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.CONVERSATION_NOT_FOUND.getCode(), response.getBody().getCode());
        assertEquals(ErrorCode.CONVERSATION_NOT_FOUND.getMessage() + ": 99", response.getBody().getMessage());
    }

    @Test
    void handleValidationShouldAggregateFieldErrors() throws Exception {
        SampleRequest request = new SampleRequest();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "request");
        bindingResult.addError(new FieldError("request", "name", "不能为空"));

        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleEndpoint", SampleRequest.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.INVALID_PARAMETER.getCode(), response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("name: 不能为空"));
    }

    @Test
    void handleMissingParamShouldIncludeParameterName() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleMissingParam(
                new MissingServletRequestParameterException("providerId", "Long")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.INVALID_PARAMETER.getCode(), response.getBody().getCode());
        assertEquals(ErrorCode.INVALID_PARAMETER.getMessage() + ": providerId", response.getBody().getMessage());
    }

    @Test
    void handleMaxUploadSizeShouldReturnPayloadTooLarge() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleMaxUploadSize(new MaxUploadSizeExceededException(1024));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals(40013, response.getBody().getCode());
        assertEquals("文件大小超过限制", response.getBody().getMessage());
    }

    @Test
    void handleUnexpectedShouldReturnInternalError() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpected(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), response.getBody().getCode());
        assertEquals(ErrorCode.INTERNAL_ERROR.getMessage(), response.getBody().getMessage());
    }

    @SuppressWarnings("unused")
    private void sampleEndpoint(SampleRequest request) {
    }

    private static class SampleRequest {
        private String name;
    }
}