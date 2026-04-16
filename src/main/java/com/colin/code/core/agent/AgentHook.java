package com.colin.code.core.agent;

import com.colin.code.core.provider.LLMResponse;
import com.colin.code.core.provider.ToolCallRequest;

import java.util.List;
import java.util.Map;

public interface AgentHook {
    default void onLoopStart(String userMessage) {}
    default void beforeLLMCall(List<Map<String, Object>> messages) {}
    default void afterLLMCall(LLMResponse response) {}
    default void onToolCall(ToolCallRequest toolCall, String result) {}
    default void onLoopEnd(String finalResponse) {}
}
