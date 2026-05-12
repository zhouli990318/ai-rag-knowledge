package com.silver.ai.domain.chat.service;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.port.HypothesisGeneratorPort;
import com.silver.ai.domain.chat.port.QueryRewriterPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class QueryPlanningDomainServiceTest {

    @Test
    void planShouldReturnOriginalVariantWhenRewriteDisabled() {
        QueryRewriterPort queryRewriter = mock(QueryRewriterPort.class);
        HypothesisGeneratorPort hypothesisGenerator = mock(HypothesisGeneratorPort.class);
        ChatOrchestratorConfig config = ChatOrchestratorConfig.builder()
                .rewriteEnabled(false)
                .multiPathRetrievalEnabled(true)
                .hydeEnabled(true)
                .build();
        QueryPlanningDomainService service = new QueryPlanningDomainService(queryRewriter, hypothesisGenerator, config);

        StepVerifier.create(service.plan("Spring AI 支持哪些向量库？", List.of("USER: 上一轮消息")))
                .assertNext(plan -> {
                    assertEquals("Spring AI 支持哪些向量库？", plan.originalQuery());
                    assertEquals("Spring AI 支持哪些向量库？", plan.rewrittenQuery());
                    assertEquals(List.of(
                            new QueryPlanningDomainService.RetrievalQueryVariant(
                                    "Spring AI 支持哪些向量库？",
                                    QueryPlanningDomainService.RetrievalQuerySource.ORIGINAL)
                    ), plan.retrievalVariants());
                })
                .verifyComplete();

        verifyNoInteractions(queryRewriter, hypothesisGenerator);
    }

    @Test
    void planShouldUseSingleRewrittenVariantWhenMultiPathRetrievalDisabled() {
        QueryRewriterPort queryRewriter = mock(QueryRewriterPort.class);
        HypothesisGeneratorPort hypothesisGenerator = mock(HypothesisGeneratorPort.class);
        ChatOrchestratorConfig config = ChatOrchestratorConfig.builder()
                .rewriteEnabled(true)
                .multiPathRetrievalEnabled(false)
                .hydeEnabled(true)
                .rewriteContextRounds(3)
                .build();
        QueryPlanningDomainService service = new QueryPlanningDomainService(queryRewriter, hypothesisGenerator, config);

        when(queryRewriter.rewrite("它的部署步骤是什么？", List.of("USER: Spring AI 是什么？", "ASSISTANT: ...")))
                .thenReturn(Mono.just("Spring AI 的部署步骤是什么？"));

        StepVerifier.create(service.plan("它的部署步骤是什么？", List.of("USER: Spring AI 是什么？", "ASSISTANT: ...")))
                .assertNext(plan -> assertEquals(List.of(
                        new QueryPlanningDomainService.RetrievalQueryVariant(
                                "Spring AI 的部署步骤是什么？",
                                QueryPlanningDomainService.RetrievalQuerySource.REWRITTEN)
                ), plan.retrievalVariants()))
                .verifyComplete();

        verify(queryRewriter).rewrite("它的部署步骤是什么？", List.of("USER: Spring AI 是什么？", "ASSISTANT: ..."));
        verifyNoInteractions(hypothesisGenerator);
    }

    @Test
    void planShouldCreateDecomposedAndHydeVariantsWhenEnabled() {
        QueryRewriterPort queryRewriter = mock(QueryRewriterPort.class);
        HypothesisGeneratorPort hypothesisGenerator = mock(HypothesisGeneratorPort.class);
        ChatOrchestratorConfig config = ChatOrchestratorConfig.builder()
                .rewriteEnabled(true)
                .multiPathRetrievalEnabled(true)
                .hydeEnabled(true)
                .rewriteContextRounds(2)
                .build();
        QueryPlanningDomainService service = new QueryPlanningDomainService(queryRewriter, hypothesisGenerator, config);

        List<String> context = List.of(
                "USER: 第一轮用户问题",
                "ASSISTANT: 第一轮回答",
                "USER: 第二轮用户问题",
                "ASSISTANT: 第二轮回答",
                "USER: 当前问题之前的更老消息"
        );

        when(queryRewriter.rewrite("它们的区别和部署方式是什么？", context.subList(0, 4)))
                .thenReturn(Mono.just("Spring AI 和 LangChain4j 的区别和部署方式是什么？"));
        when(queryRewriter.decompose("Spring AI 和 LangChain4j 的区别和部署方式是什么？"))
                .thenReturn(Mono.just(List.of(
                        "Spring AI 和 LangChain4j 的区别",
                        "Spring AI 的部署方式",
                        "LangChain4j 的部署方式",
                        "Spring AI 和 LangChain4j 的区别"
                )));
        when(hypothesisGenerator.generate(
                "Spring AI 和 LangChain4j 的区别和部署方式是什么？",
                context.subList(0, 4)))
                .thenReturn(Mono.just("Spring AI 更适合 Spring 生态集成，LangChain4j 更轻量。"));

        StepVerifier.create(service.plan("它们的区别和部署方式是什么？", context))
                .assertNext(plan -> assertEquals(List.of(
                        new QueryPlanningDomainService.RetrievalQueryVariant(
                                "Spring AI 和 LangChain4j 的区别",
                                QueryPlanningDomainService.RetrievalQuerySource.DECOMPOSED),
                        new QueryPlanningDomainService.RetrievalQueryVariant(
                                "Spring AI 的部署方式",
                                QueryPlanningDomainService.RetrievalQuerySource.DECOMPOSED),
                        new QueryPlanningDomainService.RetrievalQueryVariant(
                                "LangChain4j 的部署方式",
                                QueryPlanningDomainService.RetrievalQuerySource.DECOMPOSED),
                        new QueryPlanningDomainService.RetrievalQueryVariant(
                                "Spring AI 更适合 Spring 生态集成，LangChain4j 更轻量。",
                                QueryPlanningDomainService.RetrievalQuerySource.HYDE)
                ), plan.retrievalVariants()))
                .verifyComplete();

        verify(queryRewriter).rewrite("它们的区别和部署方式是什么？", context.subList(0, 4));
        verify(queryRewriter).decompose("Spring AI 和 LangChain4j 的区别和部署方式是什么？");
        verify(hypothesisGenerator).generate("Spring AI 和 LangChain4j 的区别和部署方式是什么？", context.subList(0, 4));
    }
}