package com.colin.code.core.message;

import com.colin.code.core.provider.LLMResponse;
import com.colin.code.core.provider.ToolCallRequest;

import java.util.List;
import java.util.Map;

public interface MessageBuilder {
    List<Map<String, Object>> buildInitial(String userMessage);
    void addAssistantMessage(List<Map<String, Object>> messages, LLMResponse response);
    void addToolResult(List<Map<String, Object>> messages, ToolCallRequest tc, String result);
}
