package com.colin.code.runtime;

import com.colin.code.core.agent.AgentHook;
import com.colin.code.core.interceptor.InputInterceptor;
import com.colin.code.core.message.MessageBuilder;
import com.colin.code.core.prompt.PromptPlugin;
import com.colin.code.core.provider.LLMProvider;
import com.colin.code.core.tool.Tool;
import com.colin.code.core.tool.ToolRegistry;
import com.colin.code.runtime.config.AppConfig;

import java.util.ArrayList;
import java.util.List;

public class CapabilityLoader {

    private final List<Tool> tools = new ArrayList<Tool>();
    private final List<PromptPlugin> promptPlugins = new ArrayList<PromptPlugin>();
    private final List<AgentHook> hooks = new ArrayList<AgentHook>();
    private final List<InputInterceptor> interceptors = new ArrayList<InputInterceptor>();
    private MessageBuilder messageBuilder;

    public CapabilityLoader withTool(Tool tool) {
        if (tool != null) {
            tools.add(tool);
        }
        return this;
    }

    public CapabilityLoader withPromptPlugin(PromptPlugin plugin) {
        if (plugin != null) {
            promptPlugins.add(plugin);
        }
        return this;
    }

    public CapabilityLoader withHook(AgentHook hook) {
        if (hook != null) {
            hooks.add(hook);
        }
        return this;
    }

    public CapabilityLoader withInterceptor(InputInterceptor interceptor) {
        if (interceptor != null) {
            interceptors.add(interceptor);
        }
        return this;
    }

    public CapabilityLoader withMessageBuilder(MessageBuilder messageBuilder) {
        this.messageBuilder = messageBuilder;
        return this;
    }

    public CapabilityLoader withCapability(StandardCapability cap, AppConfig config) {
        Object instance = cap.create(config);
        switch (cap.getType()) {
            case TOOL:
                withTool((Tool) instance);
                break;
            case PROMPT_PLUGIN:
                withPromptPlugin((PromptPlugin) instance);
                break;
            case HOOK:
                withHook((AgentHook) instance);
                break;
            case INTERCEPTOR:
                withInterceptor((InputInterceptor) instance);
                break;
            default:
                break;
        }
        return this;
    }

    public AgentRuntime build(LLMProvider provider, AppConfig config) {
        ToolRegistry registry = new ToolRegistry();
        for (Tool t : tools) {
            registry.register(t);
        }

        MessageBuilder mb = this.messageBuilder;
        if (mb == null) {
            mb = new com.colin.code.capability.prompt.StandardMessageBuilder(promptPlugins, config);
        }

        com.colin.code.core.agent.AgentLoop loop = new com.colin.code.core.agent.AgentLoop(
            provider, registry, mb, hooks, interceptors, config
        );

        return new AgentRuntime(loop);
    }
}
