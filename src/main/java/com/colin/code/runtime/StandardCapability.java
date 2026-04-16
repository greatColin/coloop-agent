package com.colin.code.runtime;

import com.colin.code.capability.hook.LoggingHook;
import com.colin.code.capability.prompt.AgentsMdPromptPlugin;
import com.colin.code.capability.prompt.BasePromptPlugin;
import com.colin.code.capability.prompt.SkillPromptPlugin;
import com.colin.code.capability.tool.exec.ExecTool;
import com.colin.code.core.agent.AgentHook;
import com.colin.code.core.interceptor.InputInterceptor;
import com.colin.code.core.prompt.PromptPlugin;
import com.colin.code.core.tool.Tool;
import com.colin.code.runtime.config.AppConfig;

import java.util.function.Function;

public enum StandardCapability {
    EXEC_TOOL(
        "exec", "Shell执行工具", "执行shell命令，返回stdout和stderr",
        CapabilityType.TOOL,
        new Function<AppConfig, Object>() {
            @Override
            public Object apply(AppConfig config) {
                return new ExecTool(config.getExecTimeoutSeconds());
            }
        }
    ),
    BASE_PROMPT(
        "base_prompt", "基础提示词", "注入身份介绍、环境信息等基础系统提示",
        CapabilityType.PROMPT_PLUGIN,
        new Function<AppConfig, Object>() {
            @Override
            public Object apply(AppConfig config) {
                return new BasePromptPlugin();
            }
        }
    ),
    SKILL_PROMPT(
        "skill_prompt", "技能提示词", "扫描并注入可用技能说明",
        CapabilityType.PROMPT_PLUGIN,
        new Function<AppConfig, Object>() {
            @Override
            public Object apply(AppConfig config) {
                return new SkillPromptPlugin();
            }
        }
    ),
    AGENTS_MD_PROMPT(
        "agents_md_prompt", "AGENTS.md提示词", "自动读取工作目录下的AGENTS.md并注入系统提示",
        CapabilityType.PROMPT_PLUGIN,
        new Function<AppConfig, Object>() {
            @Override
            public Object apply(AppConfig config) {
                return new AgentsMdPromptPlugin();
            }
        }
    ),
    LOGGING_HOOK(
        "logging", "日志钩子", "在Agent Loop关键生命周期节点打印调试日志",
        CapabilityType.HOOK,
        new Function<AppConfig, Object>() {
            @Override
            public Object apply(AppConfig config) {
                return new LoggingHook();
            }
        }
    );

    private final String id;
    private final String name;
    private final String description;
    private final CapabilityType type;
    private final Function<AppConfig, Object> factory;

    StandardCapability(String id, String name, String description, CapabilityType type, Function<AppConfig, Object> factory) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.factory = factory;
    }

    public Object create(AppConfig config) {
        return factory.apply(config);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public CapabilityType getType() { return type; }

    public static StandardCapability fromId(String id) {
        for (StandardCapability c : values()) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown capability: " + id);
    }
}
