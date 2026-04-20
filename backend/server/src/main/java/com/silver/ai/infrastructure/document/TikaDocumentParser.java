package com.silver.ai.infrastructure.document;

import com.silver.ai.domain.knowledge.port.DocumentParserPort;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@SuppressWarnings("null")
public class TikaDocumentParser implements DocumentParserPort {

    @Override
    public List<String> parse(InputStream inputStream, String fileName) {
        try {
            var resource = new InputStreamResource(inputStream) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            return reader.get().stream()
                    .map(org.springframework.ai.document.Document::getText)
                    .filter(text -> text != null && !text.isBlank())
                    .toList();
        } catch (Exception e) {
            log.error("Failed to parse document: {}", fileName, e);
            throw new BusinessException(ErrorCode.DOCUMENT_PARSE_FAILED, fileName, e);
        }
    }
}
