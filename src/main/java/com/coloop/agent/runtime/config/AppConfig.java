package com.coloop.agent.runtime.config;

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

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiKey() {
        return apiKey != null ? apiKey : "";
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiBase() {
        return apiBase;
    }

    public void setApiBase(String apiBase) {
        this.apiBase = apiBase;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public int getExecTimeoutSeconds() {
        return execTimeoutSeconds;
    }

    public void setExecTimeoutSeconds(int execTimeoutSeconds) {
        this.execTimeoutSeconds = execTimeoutSeconds;
    }

    /**
     * 从环境变量加载配置，返回一个新的 AppConfig 实例。
     * 优先读取 COLIN_CODE_ 前缀的变量，其次读取 OPENAI_ 前缀的变量。
     * 支持的环境变量：
     * - COLIN_CODE_OPENAI_MODEL / OPENAI_MODEL
     * - COLIN_CODE_OPENAI_API_KEY / OPENAI_API_KEY
     * - COLIN_CODE_OPENAI_API_BASE / OPENAI_API_BASE
     */
    public static AppConfig fromEnv() {
        AppConfig config = new AppConfig();
        String model = System.getenv("COLIN_CODE_OPENAI_MODEL");
        if (model == null) model = System.getenv("OPENAI_MODEL");

        String apiKey = System.getenv("COLIN_CODE_OPENAI_API_KEY");
        if (apiKey == null) apiKey = System.getenv("OPENAI_API_KEY");

        String apiBase = System.getenv("COLIN_CODE_OPENAI_API_BASE");
        if (apiBase == null) apiBase = System.getenv("OPENAI_API_BASE");

        if (model != null) config.setModel(model);
        if (apiKey != null) config.setApiKey(apiKey);
        if (apiBase != null) config.setApiBase(apiBase);
        return config;
    }
}
