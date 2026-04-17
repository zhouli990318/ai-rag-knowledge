package com.silver.ai.mcpgateway.application;

import com.silver.ai.mcpgateway.domain.model.ApiSource;
import com.silver.ai.mcpgateway.domain.model.AuthType;
import com.silver.ai.mcpgateway.domain.model.ToolMapping;
import com.silver.ai.mcpgateway.domain.port.ApiSourceRepository;
import com.silver.ai.mcpgateway.domain.port.OpenApiParserPort;
import com.silver.ai.mcpgateway.domain.port.ToolMappingRepository;
import com.silver.ai.mcpgateway.domain.service.ToolInvocationDomainService;
import com.silver.ai.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpGatewayAppServiceTest {

    @Test
    void createApiSourceShouldParseAndSaveToolsWhenSpecProvided() {
        ApiSourceRepository sourceRepository = mock(ApiSourceRepository.class);
        ToolMappingRepository toolRepository = mock(ToolMappingRepository.class);
        OpenApiParserPort parser = mock(OpenApiParserPort.class);
        ToolInvocationDomainService invocationService = mock(ToolInvocationDomainService.class);
        McpGatewayAppService service = new McpGatewayAppService(sourceRepository, toolRepository, parser, invocationService);
        ApiSource savedSource = ApiSource.builder().id(10L).name("demo").build();
        ToolMapping mapping = ToolMapping.builder().toolName("tool-1").build();
        when(sourceRepository.save(any(ApiSource.class))).thenReturn(savedSource);
        when(parser.parse("spec", 10L)).thenReturn(List.of(mapping));
        when(toolRepository.save(mapping)).thenReturn(mapping);

        ApiSource result = service.createApiSource("demo", "desc", "https://api.example.com", AuthType.NONE, null, "spec");

        assertEquals(10L, result.getId());
        verify(parser).parse("spec", 10L);
        verify(toolRepository).save(mapping);
    }

    @Test
    void parseOpenApiSpecShouldUpdateSpecDeleteOldMappingsAndSaveNewOnes() {
        ApiSourceRepository sourceRepository = mock(ApiSourceRepository.class);
        ToolMappingRepository toolRepository = mock(ToolMappingRepository.class);
        OpenApiParserPort parser = mock(OpenApiParserPort.class);
        McpGatewayAppService service = new McpGatewayAppService(sourceRepository, toolRepository, parser, mock(ToolInvocationDomainService.class));
        ApiSource source = ApiSource.builder().id(3L).name("demo").build();
        ToolMapping mapping = ToolMapping.builder().toolName("new-tool").build();
        when(sourceRepository.findById(3L)).thenReturn(Optional.of(source));
        when(sourceRepository.save(any(ApiSource.class))).thenReturn(source);
        when(parser.parse("new-spec", 3L)).thenReturn(List.of(mapping));
        when(toolRepository.save(mapping)).thenReturn(mapping);

        List<ToolMapping> result = service.parseOpenApiSpec(3L, "new-spec");

        assertEquals(1, result.size());
        assertEquals("new-spec", source.getOpenApiSpec());
        verify(toolRepository).deleteByApiSourceId(3L);
        verify(toolRepository).save(mapping);
    }

    @Test
    void invokeToolShouldFailWhenToolDoesNotExist() {
        ToolMappingRepository toolRepository = mock(ToolMappingRepository.class);
        McpGatewayAppService service = new McpGatewayAppService(mock(ApiSourceRepository.class), toolRepository,
                mock(OpenApiParserPort.class), mock(ToolInvocationDomainService.class));
        when(toolRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.invokeTool(99L, "{}"));

        assertEquals("MCP工具不存在", exception.getErrorCode().getMessage());
    }

    @Test
    void createApiSourceShouldNormalizeBaseUrlWithoutScheme() {
        ApiSourceRepository sourceRepository = mock(ApiSourceRepository.class);
        McpGatewayAppService service = new McpGatewayAppService(sourceRepository, mock(ToolMappingRepository.class),
                mock(OpenApiParserPort.class), mock(ToolInvocationDomainService.class));
        ApiSource savedSource = ApiSource.builder().id(10L).name("demo").baseUrl("http://192.168.9.148:8080/api").build();
        when(sourceRepository.save(any(ApiSource.class))).thenReturn(savedSource);

        ApiSource result = service.createApiSource("demo", "desc", "192.168.9.148:8080/api", AuthType.NONE, null, null);

        assertEquals("http://192.168.9.148:8080/api", result.getBaseUrl());
        verify(sourceRepository).save(argThat(source -> "http://192.168.9.148:8080/api".equals(source.getBaseUrl())));
    }

    @Test
    void createApiSourceShouldRejectInvalidBaseUrl() {
        McpGatewayAppService service = new McpGatewayAppService(mock(ApiSourceRepository.class), mock(ToolMappingRepository.class),
                mock(OpenApiParserPort.class), mock(ToolInvocationDomainService.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createApiSource("demo", "desc", "http://bad host", AuthType.NONE, null, null));

        assertEquals("参数校验失败", exception.getErrorCode().getMessage());
    }
}