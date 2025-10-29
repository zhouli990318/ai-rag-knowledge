package com.silver.mcpserver.infrastructure.adapter;

import com.silver.mcpserver.domain.adapter.INoticePort;
import com.silver.mcpserver.domain.model.request.NoticeFunctionRequest;
import com.silver.mcpserver.domain.model.response.NoticeFunctionResponse;
import com.silver.mcpserver.types.properties.WeChatApiProperties;
import jakarta.annotation.Resource;

public class WeChatNoticePort implements INoticePort {

    @Resource
    private WeChatApiProperties weChatApiProperties;

    @Resource
    private WeChat

    @Override
    public NoticeFunctionResponse noticeMessage(NoticeFunctionRequest request) {
        return null;
    }
}
