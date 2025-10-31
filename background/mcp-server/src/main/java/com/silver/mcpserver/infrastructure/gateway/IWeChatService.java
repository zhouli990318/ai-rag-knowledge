package com.silver.mcpserver.infrastructure.gateway;

import com.silver.mcpserver.infrastructure.gateway.dto.WeChatTemplateMessageDTO;
import com.silver.mcpserver.infrastructure.gateway.dto.WeChatTokenResponseDTO;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface IWeChatService {

    @GET("cgi-bin/token")
    Call<WeChatTokenResponseDTO> getAccessToken(@Query("grant_type") String grantType, @Query("appid") String appid, @Query("secret") String secret);

    @POST("cgi-bin/message/template/send")
    Call<Void> sendMessage(@Query("access_token")String token, @Body WeChatTemplateMessageDTO templateMessageDTO);
}
