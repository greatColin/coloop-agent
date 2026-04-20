package com.coloop.agent.capability.hook;

import com.coloop.agent.core.agent.AgentHook;
import com.coloop.agent.core.provider.LLMResponse;
import com.coloop.agent.core.provider.ToolCallRequest;

import java.util.List;
import java.util.Map;

public class LoggingHook implements AgentHook {
    @Override
    public void onLoopStart(String userMessage) {
        System.out.println("[LOG] Loop start: " + userMessage);
    }

    @Override
    public void beforeLLMCall(List<Map<String, Object>> messages) {
//        System.out.println("[LOG] Before LLM call, messages: " + messages.size());
    }

    @Override
    public void afterLLMCall(LLMResponse response) {
//        System.out.println("[LOG] After LLM call, hasToolCalls: " + response.hasToolCalls());
    }

    @Override
    public void onToolCall(ToolCallRequest toolCall, String result, String formattedArgs) {
        if (formattedArgs != null && !formattedArgs.isEmpty()) {
            System.out.println("[LOG] Tool executed: " + toolCall.getName() + "(" + formattedArgs + ")");
        } else {
            System.out.println("[LOG] Tool executed: " + toolCall.getName());
        }
    }

    @Override
    public void onThinking(String content, String reasoningContent) {
        if (reasoningContent != null && !reasoningContent.isEmpty()) {
            System.out.println("[THINKING] " + reasoningContent);
        }
        if (content != null && !content.isEmpty()) {
            System.out.println("[THINKING] " + content);
        }
    }

    @Override
    public void onLoopEnd(boolean maxIte, String finalResponse) {
        if(maxIte) {
            System.out.println(finalResponse);
        } else {
            System.out.println("[LOG] Loop end.");
        }
    }
}
