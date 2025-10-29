package com.silver.mcpserver.domain.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.silver.mcpserver.types.utils.MarkdownConverter;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArticleFunctionRequest {

    @JsonProperty(required = true, value = "title")
    @JsonPropertyDescription("文章标题")
    private String title;

    @JsonProperty(required = true, value = "markdownContent")
    @JsonPropertyDescription("文章内容")
    private String markdownContent;

    @JsonProperty(required = true, value = "tags")
    @JsonPropertyDescription("文章标签，英文逗号隔开")
    private String tags;

    @JsonProperty(required = true, value = "Description")
    @JsonPropertyDescription("文章简述")
    private String Description;

    public String getContent() {
        return MarkdownConverter.convertToHtml(markdownContent);
    }

}
