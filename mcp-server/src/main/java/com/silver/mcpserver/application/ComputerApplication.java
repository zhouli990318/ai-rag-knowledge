package com.silver.mcpserver.application;

import com.silver.mcpserver.domain.ComputerFunctionRequest;
import com.silver.mcpserver.domain.ComputerFunctionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Slf4j
@Component
public class ComputerApplication {

    public ComputerFunctionResponse queryConfig() {
        // 获取系统属性
        Properties properties = System.getProperties();

        // 操作系统名称
        String osName = properties.getProperty("os.name");
        // 操作系统版本
        String osVersion = properties.getProperty("os.version");
        // 操作系统架构
        String osArch = properties.getProperty("os.arch");
        // 用户的账户名称
        String userName = properties.getProperty("user.name");
        // 用户的主目录
        String userHome = properties.getProperty("user.home");
        // 用户的当前工作目录
        String userDir = properties.getProperty("user.dir");
        // Java 运行时环境版本
        String javaVersion = properties.getProperty("java.version");

        String osInfo = "";
        // 根据操作系统执行特定的命令来获取更多信息
        if (osName.toLowerCase().contains("win")) {
            // Windows特定的代码
            osInfo = getWindowsSpecificInfo();
        } else if (osName.toLowerCase().contains("mac")) {
            // macOS特定的代码
            osInfo = getMacSpecificInfo();
        } else if (osName.toLowerCase().contains("nix") || osName.toLowerCase().contains("nux")) {
            // Linux特定的代码
            osInfo = getLinuxSpecificInfo();
        }

        ComputerFunctionResponse response = new ComputerFunctionResponse();
        response.setOsName(osName);
        response.setOsVersion(osVersion);
        response.setOsArch(osArch);
        response.setUserName(userName);
        response.setUserHome(userHome);
        response.setUserDir(userDir);
        response.setJavaVersion(javaVersion);
        response.setOsInfo(osInfo);

        log.info("获取电脑配置信息 {}", response);

        return response;
    }

    private String getWindowsSpecificInfo() {
        StringBuilder cache = new StringBuilder();
        // Windows特定的系统信息获取
        try {
            Process process = new ProcessBuilder("systeminfo").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));
            String line;
            while ((line = reader.readLine()) != null) {
                cache.append(line);
            }
        } catch (Exception e) {
            log.error("获取Windows系统信息失败", e);
        }
        return cache.toString();
    }

    private static String getMacSpecificInfo() {
        StringBuilder cache = new StringBuilder();
        // macOS特定的系统信息获取
        try {
            Process process = Runtime.getRuntime().exec("system_profiler SPHardwareDataType");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                cache.append(line);
            }
        } catch (Exception e) {
            log.error("获取Mac系统信息失败", e);
        }
        return cache.toString();
    }

    private static String getLinuxSpecificInfo() {
        StringBuilder cache = new StringBuilder();
        // Linux特定的系统信息获取
        try {
            Process process = Runtime.getRuntime().exec("lshw -short");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                cache.append(line);
            }
        } catch (Exception e) {
            log.error("获取Linux系统信息失败", e);
        }
        return cache.toString();
    }
}
