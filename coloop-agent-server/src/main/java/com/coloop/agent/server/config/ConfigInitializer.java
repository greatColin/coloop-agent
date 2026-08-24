package com.coloop.agent.server.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时初始化配置库：建表并导入引导默认值。
 */
@Component
public class ConfigInitializer implements ApplicationRunner {

    private final SqliteConfigRepository repository;

    public ConfigInitializer(SqliteConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        repository.initSchema();
        repository.seedFromSetting();
    }
}
