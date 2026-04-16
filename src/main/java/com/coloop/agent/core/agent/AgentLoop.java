package com.coloop.agent.core.agent;

import com.coloop.agent.core.interceptor.InputInterceptor;
import com.coloop.agent.core.message.MessageBuilder;
import com.coloop.agent.core.provider.LLMProvider;
import com.coloop.agent.core.provider.LLMResponse;
import com.coloop.agent.core.provider.ToolCallRequest;
import com.coloop.agent.core.tool.ToolRegistry;
import com.coloop.agent.runtime.config.AppConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AgentLoop {

    private final LLMProvider provider;
    private final ToolRegistry toolRegistry;
    private final MessageBuilder messageBuilder;
    private final List<AgentHook> hooks;
    private final List<InputInterceptor> interceptors;
    private final AppConfig config;

    public AgentLoop(
            LLMProvider provider,
            ToolRegistry toolRegistry,
            MessageBuilder messageBuilder,
            List<AgentHook> hooks,
            List<InputInterceptor> interceptors,
            AppConfig config) {
        this.provider = provider;
        this.toolRegistry = toolRegistry;
        this.messageBuilder = messageBuilder;
        this.hooks = hooks != null ? hooks : Collections.<AgentHook>emptyList();
        this.interceptors = interceptors != null ? interceptors : Collections.<InputInterceptor>emptyList();
        this.config = config;
    }

    public String chat(String userMessage) {
        for (AgentHook h : hooks) {
            h.onLoopStart(userMessage);
        }

        for (InputInterceptor ic : interceptors) {
            Optional<String> direct = ic.intercept(userMessage);
            if (direct.isPresent()) {
                for (AgentHook h : hooks) {
                    h.onLoopEnd(direct.get());
                }
                return direct.get();
            }
        }

        List<Map<String, Object>> messages = messageBuilder.buildInitial(userMessage);

        for (int iter = 0; iter < config.getMaxIterations(); iter++) {
            for (AgentHook h : hooks) {
                h.beforeLLMCall(messages);
            }
            LLMResponse response = provider.chat(
                    messages,
                    toolRegistry.getDefinitions(),
                    config.getModel(),
                    config.getMaxTokens(),
                    config.getTemperature()
            );
            for (AgentHook h : hooks) {
                h.afterLLMCall(response);
            }

            if (response.hasToolCalls()) {
                messageBuilder.addAssistantMessage(messages, response);
                for (ToolCallRequest tc : response.getToolCalls()) {
                    String result = toolRegistry.execute(tc);
                    for (AgentHook h : hooks) {
                        h.onToolCall(tc, result);
                    }
                    messageBuilder.addToolResult(messages, tc, result);
                }
            } else {
                String finalResponse = response.getContent() != null ? response.getContent() : "";
                for (AgentHook h : hooks) {
                    h.onLoopEnd(finalResponse);
                }
                return finalResponse;
            }
        }

        String maxIterMsg = "[Reached max iterations: " + config.getMaxIterations() + "]";
        for (AgentHook h : hooks) {
            h.onLoopEnd(maxIterMsg);
        }
        return maxIterMsg;
    }
}
