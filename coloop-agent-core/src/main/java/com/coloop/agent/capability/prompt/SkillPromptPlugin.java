package com.coloop.agent.capability.prompt;

import com.coloop.agent.core.prompt.PromptPlugin;
import com.coloop.agent.runtime.config.AppConfig;

import java.util.Map;

public class SkillPromptPlugin implements PromptPlugin {
    @Override
    public String getName() { return "skill"; }

    @Override
    public int getPriority() { return 10; }

    @Override
    public String generate(AppConfig config, Map<String, Object> ctx) {
        return "";
    }
}
