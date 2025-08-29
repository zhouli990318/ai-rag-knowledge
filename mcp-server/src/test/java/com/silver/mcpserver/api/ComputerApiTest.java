package com.silver.mcpserver.api;

import com.silver.mcpserver.domain.ComputerFunctionRequest;
import com.silver.mcpserver.domain.ComputerFunctionResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class ComputerApiTest {

    @Resource
    private ComputerApi computerApi;

    @Test
    void queryComputerConfig_ShouldReturnSystemProperties_WhenComputerNameIsEmpty() {
        // Arrange
        ComputerFunctionRequest request = new ComputerFunctionRequest();
        request.setComputer("");

        // Act
        ComputerFunctionResponse response = computerApi.queryComputerConfig();

        // Assert
        assertNotNull(response);
        assertNotNull(response.getOsName());
        assertNotNull(response.getOsVersion());
        assertNotNull(response.getOsArch());
        assertNotNull(response.getUserName());
        assertNotNull(response.getUserHome());
        assertNotNull(response.getUserDir());
        assertNotNull(response.getJavaVersion());
        assertNotNull(response.getOsInfo());
    }

    @Test
    void queryComputerConfig_ShouldReturnSystemProperties_WhenComputerNameIsProvided() {
        // Arrange
        ComputerFunctionRequest request = new ComputerFunctionRequest();
        request.setComputer("TestPC");

        // Act
        ComputerFunctionResponse response = computerApi.queryComputerConfig();

        // Assert
        assertNotNull(response);
        assertNotNull(response.getOsName());
        assertNotNull(response.getOsVersion());
        assertNotNull(response.getOsArch());
        assertNotNull(response.getUserName());
        assertNotNull(response.getUserHome());
        assertNotNull(response.getUserDir());
        assertNotNull(response.getJavaVersion());
        assertNotNull(response.getOsInfo());
    }
}