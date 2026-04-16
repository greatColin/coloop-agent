package com.colin.code.runtime.config;

/**
 * 应用配置：模型参数、API 连接、执行限制等。
 */
public class AppConfig {

    private String model = "";
    private String apiKey = "";
    private String apiBase = "";

    private int maxTokens = 2048;
    private double temperature = 0.7;
    private int maxIterations = 10;
    private int execTimeoutSeconds = 30;

    public String getModel() {
        return model;
    }

    public String getApiKey() {
        return apiKey != null ? apiKey : "";
    }

    public String getApiBase() {
        return apiBase;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public int getExecTimeoutSeconds() {
        return execTimeoutSeconds;
    }
}
