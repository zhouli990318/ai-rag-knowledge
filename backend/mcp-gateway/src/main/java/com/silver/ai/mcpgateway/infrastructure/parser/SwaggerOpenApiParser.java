package com.silver.ai.mcpgateway.infrastructure.parser;

import com.silver.ai.mcpgateway.domain.model.ToolMapping;
import com.silver.ai.mcpgateway.domain.port.OpenApiParserPort;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SwaggerOpenApiParser implements OpenApiParserPort {

    private final ObjectMapper objectMapper;

    @Override
    public List<ToolMapping> parse(String openApiSpec, Long apiSourceId) {
        try {
            ParseOptions options = new ParseOptions();
            options.setResolve(true);
            options.setResolveFully(true);

            SwaggerParseResult result = new OpenAPIV3Parser().readContents(openApiSpec, null, options);

            if (result.getOpenAPI() == null) {
                String errors = result.getMessages() != null ? String.join("; ", result.getMessages()) : "Unknown error";
                throw new BusinessException(ErrorCode.MCP_PARSE_FAILED, errors);
            }

            return extractToolMappings(result.getOpenAPI(), apiSourceId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse OpenAPI spec", e);
            throw new BusinessException(ErrorCode.MCP_PARSE_FAILED, e.getMessage(), e);
        }
    }

    @Override
    public List<ToolMapping> parseFromUrl(String url, Long apiSourceId) {
        try {
            ParseOptions options = new ParseOptions();
            options.setResolve(true);
            options.setResolveFully(true);

            SwaggerParseResult result = new OpenAPIV3Parser().readLocation(url, null, options);

            if (result.getOpenAPI() == null) {
                String errors = result.getMessages() != null ? String.join("; ", result.getMessages()) : "Unknown error";
                throw new BusinessException(ErrorCode.MCP_PARSE_FAILED, errors);
            }

            return extractToolMappings(result.getOpenAPI(), apiSourceId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.MCP_PARSE_FAILED, e.getMessage(), e);
        }
    }

    private List<ToolMapping> extractToolMappings(OpenAPI openAPI, Long apiSourceId) {
        List<ToolMapping> mappings = new ArrayList<>();

        if (openAPI.getPaths() == null) return mappings;

        for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
            String path = pathEntry.getKey();
            PathItem pathItem = pathEntry.getValue();

            extractOperation(apiSourceId, path, "GET", pathItem.getGet(), mappings);
            extractOperation(apiSourceId, path, "POST", pathItem.getPost(), mappings);
            extractOperation(apiSourceId, path, "PUT", pathItem.getPut(), mappings);
            extractOperation(apiSourceId, path, "DELETE", pathItem.getDelete(), mappings);
            extractOperation(apiSourceId, path, "PATCH", pathItem.getPatch(), mappings);
        }

        log.info("Parsed {} tool mappings from OpenAPI spec", mappings.size());
        return mappings;
    }

    private void extractOperation(Long apiSourceId, String path, String method,
                                   Operation operation, List<ToolMapping> mappings) {
        if (operation == null) return;

        String operationId = operation.getOperationId();
        if (operationId == null || operationId.isBlank()) {
            operationId = method.toLowerCase() + path.replaceAll("[^a-zA-Z0-9]", "_");
        }

        String description = operation.getSummary();
        if (description == null) description = operation.getDescription();
        if (description == null) description = operationId;

        String paramSchema = buildParameterSchema(operation);

        ToolMapping mapping = ToolMapping.builder()
                .apiSourceId(apiSourceId)
                .operationId(operationId)
                .toolName(sanitizeToolName(operationId))
                .toolDescription(description)
                .httpMethod(method)
                .path(path)
                .parameterSchema(paramSchema)
                .enabled(true)
                .build();

        mappings.add(mapping);
    }

    private String buildParameterSchema(Operation operation) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        // Path / Query / Header 参数
        if (operation.getParameters() != null) {
            for (Parameter param : operation.getParameters()) {
                Map<String, Object> prop = new LinkedHashMap<>();
                prop.put("type", getSchemaType(param.getSchema()));
                prop.put("description", param.getDescription() != null ? param.getDescription() : param.getName());
                prop.put("in", param.getIn());
                properties.put(param.getName(), prop);
                if (Boolean.TRUE.equals(param.getRequired())) {
                    required.add(param.getName());
                }
            }
        }

        // Request Body
        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
            var jsonContent = operation.getRequestBody().getContent().get("application/json");
            if (jsonContent != null && jsonContent.getSchema() != null) {
                Schema<?> bodySchema = jsonContent.getSchema();
                if (bodySchema.getProperties() != null) {
                    for (Map.Entry<String, ?> entry : bodySchema.getProperties().entrySet()) {
                        if (!(entry.getValue() instanceof Schema<?> propertySchema)) {
                            continue;
                        }
                        Map<String, Object> prop = new LinkedHashMap<>();
                        prop.put("type", getSchemaType(propertySchema));
                        prop.put("description", propertySchema.getDescription() != null
                                ? propertySchema.getDescription() : entry.getKey());
                        prop.put("in", "body");
                        properties.put(entry.getKey(), prop);
                    }
                    if (bodySchema.getRequired() != null) {
                        required.addAll(bodySchema.getRequired());
                    }
                }
            }
        }

        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }

        try {
            return objectMapper.writeValueAsString(schema);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String getSchemaType(Schema<?> schema) {
        if (schema == null) return "string";
        return schema.getType() != null ? schema.getType() : "string";
    }

    private String sanitizeToolName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}
