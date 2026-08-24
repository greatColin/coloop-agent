package com.coloop.agent.runtime.config;

import java.util.Optional;

/**
 * 配置持久化抽象。负责将 {@link AppConfig} 持久化到外部存储（如 SQLite）。
 * 接口定义于 core 模块以保持核心精简，具体实现由 server 模块提供。
 */
public interface ConfigRepository {

    /**
     * 加载已持久化的配置。
     *
     * @return 持久化的配置；无数据时返回 {@link Optional#empty()}
     */
    Optional<AppConfig> load();

    /**
     * 保存配置；已存在则覆盖旧配置。
     *
     * @param config 待保存的配置
     */
    void save(AppConfig config);

    /**
     * 是否存在已持久化的配置数据。
     *
     * @return true 表示已有持久化配置
     */
    boolean hasStoredConfig();
}
