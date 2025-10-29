package com.silver.mcpserver.api;

import com.silver.mcpserver.application.ComputerApplication;
import com.silver.mcpserver.domain.model.response.ComputerFunctionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ComputerApi {

    private final ComputerApplication computerApplication;

    public ComputerApi(ComputerApplication computerApplication) {
        this.computerApplication = computerApplication;
    }

    @Tool(description = "获取电脑配置")
    public ComputerFunctionResponse queryComputerConfig() {
        return computerApplication.queryConfig();
    }

}
