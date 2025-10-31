package com.silver.mcpserver.infrastructure.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

@Data
public class WeChatTemplateMessageDTO {

    @JsonProperty("touser")
    private String toUser;
    @JsonProperty("template_id")
    private String templateId;
    @JsonProperty("url")
    private String url;
    @JsonProperty("data")
    private Map<String, Map<String, String>> data = new HashMap<>();

    public WeChatTemplateMessageDTO(String toUser, String templateId) {
        this.toUser = toUser;
        this.templateId = templateId;
    }

    public void put(TemplateKey key, String value) {
        data.put(key.getCode(), new HashMap<>() {
            @Serial
            private static final long serialVersionUID = 7092338402387318563L;

            {
                put("value", value);
            }
        });
    }


    @Getter
    public enum TemplateKey {
        platform("platformName","平台"),
        subject("subjectName","主题"),
        description("descriptionName","简述"),
        ;

        private final String code;
        private final String desc;

        TemplateKey(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }
}
