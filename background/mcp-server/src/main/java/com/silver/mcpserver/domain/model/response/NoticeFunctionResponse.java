package com.silver.mcpserver.domain.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoticeFunctionResponse {

    @JsonProperty(required = true, value = "success")
    @JsonPropertyDescription("success")
    private Boolean success;

}
