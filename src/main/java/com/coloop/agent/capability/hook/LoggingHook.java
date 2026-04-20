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
        System.out.println("[LOG] Before LLM call, messages: " + messages.size());
    }

    @Override
    public void afterLLMCall(LLMResponse response) {
        System.out.println("[LOG] After LLM call, hasToolCalls: " + response.hasToolCalls());
    }

    @Override
    public void onToolCall(ToolCallRequest toolCall, String result) {
        String argsPreview = formatArgsPreview(toolCall.getArguments());
        System.out.println("[LOG] Tool executed: " + toolCall.getName() + "(" + argsPreview + ")");
    }

    private String formatArgsPreview(Map<String, Object> args) {
        if (args == null || args.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            String value = String.valueOf(entry.getValue());
            if (value.length() > 15) {
                value = value.substring(0, 12) + "...";
            }
            sb.append(entry.getKey()).append("=").append(value);
        }
        String result = sb.toString();
        return result.length() > 30 ? result.substring(0, 27) + "..." : result;
    }

    @Override
    public void onLoopEnd(String finalResponse) {
        System.out.println("[LOG] Loop end.");
    }
}
