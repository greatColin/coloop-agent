package com.coloop.agent.core.prompt;

import com.coloop.agent.runtime.config.AppConfig;

import java.util.Map;

/**
 * Prompt 插件：负责贡献一段 system prompt 内容。
 *
 * <p>多个 PromptPlugin 实例按优先级排序后，由 {@link com.coloop.agent.capability.prompt.StandardMessageBuilder}
 * 拼接成完整的 system message。</p>
 */
public interface PromptPlugin {

    /** 返回插件名称，用于调试和日志。 */
    String getName();

    /**
     * 返回优先级，数字越小越靠前。
     * 建议值：基础提示词 0，技能注入 10，项目约定 20。
     */
    int getPriority();

    /**
     * 根据运行时上下文生成 prompt 段落。
     *
     * @param config 应用配置（仅读取，不应修改）
     * @param runtimeContext 运行时上下文，如 cwd、os、time 等
     * @return 该插件贡献的 prompt 段落；若返回 null 或空字符串，则被忽略
     */
    String generate(AppConfig config, Map<String, Object> runtimeContext);
}
