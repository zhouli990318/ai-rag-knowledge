package com.silver.ai.infrastructure.config;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.port.*;
import com.silver.ai.domain.chat.service.*;
import com.silver.ai.domain.knowledge.port.*;
import com.silver.ai.domain.knowledge.service.*;
import com.silver.ai.domain.provider.port.ModelProviderRepository;
import com.silver.ai.domain.provider.service.ModelRoutingDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 领域服务 Bean 注册 — 将领域服务从 Spring @Service 注解解耦，
 * 通过显式 @Bean 方法注册，保持领域层纯净。
 */
@Configuration
public class DomainServiceConfig {

    @Bean
    public IntentDecisionDomainService intentDecisionDomainService(
            IntentClassifierPort intentClassifier,
            IntentNodeRepository intentNodeRepository,
            ChatOrchestratorConfig config) {
        return new IntentDecisionDomainService(intentClassifier, intentNodeRepository, config);
    }

    @Bean
    public ToolRoutingDomainService toolRoutingDomainService(
            McpToolPort mcpToolPort,
            ToolIndexDomainService toolIndexService,
            ChatOrchestratorConfig config) {
        return new ToolRoutingDomainService(mcpToolPort, toolIndexService, config);
    }

    @Bean
    public QueryPlanningDomainService queryPlanningDomainService(
            QueryRewriterPort queryRewriter,
            ChatOrchestratorConfig config) {
        return new QueryPlanningDomainService(queryRewriter, config);
    }

    @Bean
    public ToolIndexDomainService toolIndexDomainService(
            ToolIndexPort toolIndexPort,
            McpToolPort mcpToolPort) {
        return new ToolIndexDomainService(toolIndexPort, mcpToolPort);
    }

    @Bean
    public DocumentProcessingDomainService documentProcessingDomainService(
            DocumentParserPort documentParser,
            TextSplitterPort textSplitter,
            VectorStorePort vectorStore,
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository) {
        return new DocumentProcessingDomainService(documentParser, textSplitter, vectorStore,
                documentRepository, documentChunkRepository);
    }

    @Bean
    public RetrievalDomainService retrievalDomainService(
            VectorStorePort vectorStore,
            KeywordSearchPort keywordSearch,
            PromptRendererPort promptRenderer) {
        return new RetrievalDomainService(vectorStore, keywordSearch, promptRenderer);
    }

    @Bean
    public MultiPathRetrievalDomainService multiPathRetrievalDomainService(
            VectorStorePort vectorStore,
            KeywordSearchPort keywordSearch,
            PromptRendererPort promptRenderer,
            ChatOrchestratorConfig config) {
        return new MultiPathRetrievalDomainService(vectorStore, keywordSearch, promptRenderer, config);
    }

    @Bean
    public EtlPipelineService etlPipelineService(
            DocumentProcessingDomainService documentProcessing,
            EtlTaskRepository etlTaskRepository) {
        return new EtlPipelineService(documentProcessing, etlTaskRepository);
    }

    @Bean
    public ModelRoutingDomainService modelRoutingDomainService(
            ModelProviderRepository providerRepository,
            ChatOrchestratorConfig config) {
        return new ModelRoutingDomainService(providerRepository, config);
    }
}
