package com.coloop.agent.core.tool;

import java.util.Map;

/**
 * 工具接口：LLM 通过名称与参数调用。
 */
public interface Tool {

    /** 工具名 */
    String getName();

    /** 工具说明，传给 LLM */
    String getDescription();

    /** JSON Schema（object），描述参数 */
    Map<String, Object> getParameters();

    /** 执行工具，返回结果字符串 */
    String execute(Map<String, Object> params);

    /**
     * 格式化参数预览，用于日志输出。
     * 默认实现：key=value, key2=value2（截断长值）。
     */
    default String formatArgsPreview(Map<String, Object> args) {
        if (args == null || args.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            String value = String.valueOf(entry.getValue());
            if (value.length() > 50) {
                value = value.substring(0, 47) + "...";
            }
            sb.append(entry.getKey()).append("=").append(value);
        }
        String result = sb.toString();
        return result.length() > 100 ? result.substring(0, 97) + "..." : result;
    }
}
