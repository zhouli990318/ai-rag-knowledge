package com.silver.mcpserver.domain.adapter;

import com.silver.mcpserver.domain.model.request.NoticeFunctionRequest;
import com.silver.mcpserver.domain.model.response.NoticeFunctionResponse;

public interface INoticePort {
    NoticeFunctionResponse noticeMessage(NoticeFunctionRequest request);
}
