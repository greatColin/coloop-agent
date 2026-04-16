package com.colin.code.core.prompt;

import com.colin.code.runtime.config.AppConfig;

import java.util.Map;

public interface PromptPlugin {
    String getName();
    int getPriority();
    String generate(AppConfig config, Map<String, Object> runtimeContext);
}
