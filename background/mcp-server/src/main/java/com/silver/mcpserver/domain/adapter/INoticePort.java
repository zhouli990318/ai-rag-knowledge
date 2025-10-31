package com.silver.mcpserver.domain.adapter;

import com.silver.mcpserver.domain.model.request.NoticeFunctionRequest;
import com.silver.mcpserver.domain.model.response.NoticeFunctionResponse;

import java.io.IOException;

public interface INoticePort {
    NoticeFunctionResponse noticeMessage(NoticeFunctionRequest request) throws IOException;
}
