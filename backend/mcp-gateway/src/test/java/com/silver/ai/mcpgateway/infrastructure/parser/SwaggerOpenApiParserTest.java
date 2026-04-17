package com.silver.ai.mcpgateway.infrastructure.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silver.ai.mcpgateway.domain.model.ToolMapping;
import com.silver.ai.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwaggerOpenApiParserTest {

    private final SwaggerOpenApiParser parser = new SwaggerOpenApiParser(new ObjectMapper());

    @Test
    void parseShouldExtractToolMappingsFromOpenApiSpec() {
        String spec = """
                openapi: 3.0.1
                info:
                  title: Demo API
                  version: 1.0.0
                paths:
                  /users/{id}:
                    get:
                      operationId: getUser
                      summary: Get user
                      parameters:
                        - name: id
                          in: path
                          required: true
                          schema:
                            type: string
                        - name: expand
                          in: query
                          schema:
                            type: string
                  /orders:
                    post:
                      summary: Create order
                      requestBody:
                        required: true
                        content:
                          application/json:
                            schema:
                              type: object
                              required: [name]
                              properties:
                                name:
                                  type: string
                                  description: order name
                """;

        List<ToolMapping> result = parser.parse(spec, 3L);

        assertEquals(2, result.size());
        assertEquals("getUser", result.get(0).getOperationId());
        assertEquals("GET", result.get(0).getHttpMethod());
        assertTrue(result.get(0).getParameterSchema().contains("expand"));
        assertEquals("post_orders", result.get(1).getToolName());
        assertTrue(result.get(1).getParameterSchema().contains("order name"));
    }

    @Test
    void parseShouldThrowWhenSpecIsInvalid() {
        assertThrows(BusinessException.class, () -> parser.parse("not-a-valid-openapi", 1L));
    }
}