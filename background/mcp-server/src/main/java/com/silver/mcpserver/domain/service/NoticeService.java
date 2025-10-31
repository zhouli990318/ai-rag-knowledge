package com.silver.mcpserver.domain.service;


import com.silver.mcpserver.domain.adapter.INoticePort;
import com.silver.mcpserver.domain.model.request.NoticeFunctionRequest;
import com.silver.mcpserver.domain.model.response.NoticeFunctionResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class NoticeService {

    @Autowired
    private INoticePort port;

    @Tool(description = "微信公众号消息通知")
    public NoticeFunctionResponse noticeMessage(NoticeFunctionRequest request) throws IOException {
        log.info("微信消息通知，平台:{} 主题:{} 描述:{}", request.getPlatform(), request.getSubject(), request.getDescription());
        return port.noticeMessage(request);
    }
}
