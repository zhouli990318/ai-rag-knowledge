package com.silver.mcpserver.wechat;

import com.silver.mcpserver.domain.adapter.INoticePort;
import com.silver.mcpserver.domain.model.request.NoticeFunctionRequest;
import com.silver.mcpserver.infrastructure.gateway.IWeChatService;
import com.silver.mcpserver.infrastructure.gateway.dto.WeChatTemplateMessageDTO;
import com.silver.mcpserver.types.properties.WeChatApiProperties;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ApiTest {

    @Resource
    private WeChatApiProperties weChatApiProperties;

    @Resource
    private IWeChatService weChatService;

    @Resource
    private INoticePort weChatNoticePort;


    @Test
    public void testWeChat() throws Exception {
        var call = weChatService.getAccessToken("client_credential", weChatApiProperties.getAppId(), weChatApiProperties.getAppSecret());
        var response = call.execute().body();
        assert response != null;
        System.out.println("WeChat Access Token: " + response.getToken());

    }

    @Test
    public void testWeChatNotice() throws Exception {
        var request = new NoticeFunctionRequest();
        request.setPlatform("测试平台");
        request.setSubject("测试主题");
        request.setDescription("这是一条来自单元测试的微信通知消息。");

        var response = weChatNoticePort.noticeMessage(request);
        assert response != null;
        System.out.println("WeChat Notice Sent Successfully");
    }
}
