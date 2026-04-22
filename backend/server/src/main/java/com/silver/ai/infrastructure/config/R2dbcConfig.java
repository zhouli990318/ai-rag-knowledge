package com.silver.ai.infrastructure.config;

import com.silver.ai.domain.chat.model.MessageRole;
import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import com.silver.ai.domain.knowledge.model.DocumentStatus;
import com.silver.ai.domain.provider.model.ProviderType;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import java.util.List;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.silver.ai.infrastructure.persistence")
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    private final ConnectionFactory connectionFactory;

    public R2dbcConfig(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public ConnectionFactory connectionFactory() {
        return connectionFactory;
    }

    @Override
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        var dialect = DialectResolver.getDialect(connectionFactory);
        return R2dbcCustomConversions.of(dialect, List.of(
                new ProviderTypeWriteConverter(),
                new ProviderTypeReadConverter(),
                new DocumentStatusWriteConverter(),
                new DocumentStatusReadConverter(),
                new ChunkTypeWriteConverter(),
                new ChunkTypeReadConverter(),
                new MessageRoleWriteConverter(),
                new MessageRoleReadConverter()
        ));
    }

    @WritingConverter
    static class ProviderTypeWriteConverter implements Converter<ProviderType, String> {
        @Override public String convert(ProviderType source) { return source.name(); }
    }
    @ReadingConverter
    static class ProviderTypeReadConverter implements Converter<String, ProviderType> {
        @Override public ProviderType convert(String source) { return ProviderType.valueOf(source); }
    }
    @WritingConverter
    static class DocumentStatusWriteConverter implements Converter<DocumentStatus, String> {
        @Override public String convert(DocumentStatus source) { return source.name(); }
    }
    @ReadingConverter
    static class DocumentStatusReadConverter implements Converter<String, DocumentStatus> {
        @Override public DocumentStatus convert(String source) { return DocumentStatus.valueOf(source); }
    }
    @WritingConverter
    static class ChunkTypeWriteConverter implements Converter<ChunkStrategy.ChunkType, String> {
        @Override public String convert(ChunkStrategy.ChunkType source) { return source.name(); }
    }
    @ReadingConverter
    static class ChunkTypeReadConverter implements Converter<String, ChunkStrategy.ChunkType> {
        @Override public ChunkStrategy.ChunkType convert(String source) { return ChunkStrategy.ChunkType.valueOf(source); }
    }
    @WritingConverter
    static class MessageRoleWriteConverter implements Converter<MessageRole, String> {
        @Override public String convert(MessageRole source) { return source.name(); }
    }
    @ReadingConverter
    static class MessageRoleReadConverter implements Converter<String, MessageRole> {
        @Override public MessageRole convert(String source) { return MessageRole.valueOf(source); }
    }
}
