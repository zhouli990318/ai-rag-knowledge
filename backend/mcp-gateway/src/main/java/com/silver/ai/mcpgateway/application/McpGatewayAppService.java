package com.silver.ai.mcpgateway.application;

import com.silver.ai.mcpgateway.domain.model.ApiSource;
import com.silver.ai.mcpgateway.domain.model.AuthType;
import com.silver.ai.mcpgateway.domain.model.ToolMapping;
import com.silver.ai.mcpgateway.domain.port.ApiSourceRepository;
import com.silver.ai.mcpgateway.domain.port.OpenApiParserPort;
import com.silver.ai.mcpgateway.domain.port.ToolMappingRepository;
import com.silver.ai.mcpgateway.domain.service.ToolInvocationDomainService;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpGatewayAppService {

    private final ApiSourceRepository apiSourceRepository;
    private final ToolMappingRepository toolMappingRepository;
    private final OpenApiParserPort openApiParser;
    private final ToolInvocationDomainService toolInvocationService;

    // ===== API Source =====

    public ApiSource createApiSource(String name, String description, String baseUrl,
                                      AuthType authType, String authConfig, String openApiSpec) {
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        ApiSource source = ApiSource.builder()
                .name(name)
                .description(description)
            .baseUrl(normalizedBaseUrl)
                .authType(authType)
                .authConfig(authConfig)
                .openApiSpec(openApiSpec)
                .build();
        source = apiSourceRepository.save(source);

        // 自动解析 OpenAPI spec
        if (openApiSpec != null && !openApiSpec.isBlank()) {
            parseAndSaveTools(source.getId(), openApiSpec);
        }

        return source;
    }

    public ApiSource updateApiSource(Long id, String name, String description, String baseUrl,
                                      AuthType authType, String authConfig) {
        ApiSource source = getApiSource(id);
        source.updateInfo(name, description, normalizeBaseUrl(baseUrl), authType, authConfig);
        return apiSourceRepository.save(source);
    }

    public ApiSource getApiSource(Long id) {
        ApiSource source = apiSourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MCP_SOURCE_NOT_FOUND));
        source.setToolMappings(toolMappingRepository.findByApiSourceId(id));
        return source;
    }

    public List<ApiSource> listApiSources() {
        return apiSourceRepository.findAll();
    }

    @Transactional
    public void deleteApiSource(Long id) {
        toolMappingRepository.deleteByApiSourceId(id);
        apiSourceRepository.deleteById(id);
    }

    // ===== OpenAPI Parsing =====

    @Transactional
    public List<ToolMapping> parseOpenApiSpec(Long apiSourceId, String openApiSpec) {
        ApiSource source = apiSourceRepository.findById(apiSourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MCP_SOURCE_NOT_FOUND));

        source.updateSpec(openApiSpec);
        apiSourceRepository.save(source);

        // 删除旧映射
        toolMappingRepository.deleteByApiSourceId(apiSourceId);

        return parseAndSaveTools(apiSourceId, openApiSpec);
    }

    @Transactional
    public List<ToolMapping> parseFromUrl(Long apiSourceId, String url) {
        List<ToolMapping> mappings = openApiParser.parseFromUrl(url, apiSourceId);
        toolMappingRepository.deleteByApiSourceId(apiSourceId);
        return mappings.stream()
                .map(toolMappingRepository::save)
                .toList();
    }

    // ===== Tool Mappings =====

    public List<ToolMapping> getToolMappings(Long apiSourceId) {
        return toolMappingRepository.findByApiSourceId(apiSourceId);
    }

    public List<ToolMapping> getAllEnabledTools() {
        return toolMappingRepository.findByEnabled(true);
    }

    public ToolMapping updateToolMapping(Long toolId, String toolName, String toolDescription,
                                         String httpMethod, String path,
                                         String parameterSchema, String responseSchema,
                                         String examplePayload,
                                         Boolean enabled) {
        ToolMapping mapping = toolMappingRepository.findById(toolId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MCP_TOOL_NOT_FOUND));
        mapping.updateConfig(toolName, toolDescription, httpMethod, path, parameterSchema, responseSchema, examplePayload);
        if (enabled != null) {
            if (enabled) mapping.enable(); else mapping.disable();
        }
        return toolMappingRepository.save(mapping);
    }

    // ===== Tool Invocation =====

    public String invokeTool(Long toolId, String arguments) {
        ToolMapping mapping = toolMappingRepository.findById(toolId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MCP_TOOL_NOT_FOUND));

        ApiSource source = apiSourceRepository.findById(mapping.getApiSourceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MCP_SOURCE_NOT_FOUND));

        return toolInvocationService.invoke(source, mapping, arguments);
    }

    private List<ToolMapping> parseAndSaveTools(Long apiSourceId, String openApiSpec) {
        List<ToolMapping> mappings = openApiParser.parse(openApiSpec, apiSourceId);
        return mappings.stream()
                .map(toolMappingRepository::save)
                .toList();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "baseUrl不能为空");
        }

        String normalized = baseUrl.trim();
        if (!normalized.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            normalized = "http://" + normalized;
        }

        HttpUrl parsedUrl = HttpUrl.parse(normalized);
        if (parsedUrl == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "baseUrl格式不合法: " + baseUrl);
        }

        String canonicalUrl = parsedUrl.toString();
        return canonicalUrl.endsWith("/") ? canonicalUrl.substring(0, canonicalUrl.length() - 1) : canonicalUrl;
    }
}
