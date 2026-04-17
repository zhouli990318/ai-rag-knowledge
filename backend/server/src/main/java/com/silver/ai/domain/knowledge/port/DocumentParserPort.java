package com.silver.ai.domain.knowledge.port;

import java.io.InputStream;
import java.util.List;

/**
 * 文档解析端口
 */
public interface DocumentParserPort {

    /**
     * 解析文档内容
     * @return 解析后的文本块列表
     */
    List<String> parse(InputStream inputStream, String fileName);
}
