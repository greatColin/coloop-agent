package com.colin.code.core.interceptor;

import java.util.Optional;

/**
 * 输入拦截器：在 AgentLoop 调用 LLM 之前检查用户输入。
 *
 * <p>用于实现快捷指令（如 <code>/compact</code>）、Skill 系统、
 * 权限确认拦截等直接返回功能。</p>
 *
 * <p>语义约定：
 * <ul>
 *   <li>返回 {@link Optional#of(Object)}：拦截该输入，直接将返回值作为最终结果，跳过 LLM 调用。</li>
 *   <li>返回 {@link Optional#empty()}：放行该输入，继续正常的 LLM 循环。</li>
 * </ul>
 * </p>
 */
public interface InputInterceptor {

    /**
     * 拦截用户输入。
     *
     * @param userMessage 原始用户输入
     * @return 若返回 Optional.of(result)，则直接返回 result 给用户；返回 Optional.empty() 则放行
     */
    Optional<String> intercept(String userMessage);
}
