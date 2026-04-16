package com.coloop.agent.core.agent;

import com.coloop.agent.core.provider.LLMResponse;
import com.coloop.agent.core.provider.ToolCallRequest;

import java.util.List;
import java.util.Map;

/**
 * Agent 生命周期钩子接口。
 *
 * <p>实现类可以在 AgentLoop 的关键节点插入自定义逻辑，如日志记录、
 * 权限检查、上下文压缩等。所有方法均有默认空实现，可按需覆写。</p>
 *
 * <p>调用顺序：
 * <ol>
 *   <li>onLoopStart</li>
 *   <li>beforeLLMCall（每轮循环）</li>
 *   <li>afterLLMCall（每轮循环）</li>
 *   <li>onToolCall（每个 tool 执行后）</li>
 *   <li>onLoopEnd（返回最终结果前）</li>
 * </ol>
 * </p>
 */
public interface AgentHook {

    /** 在 AgentLoop 开始处理用户消息时调用。 */
    default void onLoopStart(String userMessage) {}

    /** 在每次调用 LLM 之前调用。 */
    default void beforeLLMCall(List<Map<String, Object>> messages) {}

    /** 在每次调用 LLM 之后调用。 */
    default void afterLLMCall(LLMResponse response) {}

    /** 在每次工具执行完成后调用。 */
    default void onToolCall(ToolCallRequest toolCall, String result) {}

    /** 在 AgentLoop 即将返回最终结果时调用。 */
    default void onLoopEnd(String finalResponse) {}
}
