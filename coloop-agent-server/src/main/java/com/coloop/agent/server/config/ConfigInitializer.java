package com.coloop.agent.server.config;

import com.coloop.agent.capability.CapabilityType;
import com.coloop.agent.runtime.StandardCapability;
import com.coloop.agent.runtime.config.AppConfig;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 启动时初始化配置库：建表、导入引导默认值，并初始化默认工具开关。
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
        initializeDefaultToolSwitches();
    }

    private void initializeDefaultToolSwitches() {
        AppConfig config = repository.load().orElse(null);
        if (config == null || (config.getToolSwitches() != null && !config.getToolSwitches().isEmpty())) {
            return;
        }
        Map<String, Boolean> switches = new HashMap<>();
        for (StandardCapability cap : StandardCapability.values()) {
            if (cap.getType() == CapabilityType.TOOL || cap.getType() == CapabilityType.COMPOSITE) {
                switches.put(cap.getId(), true);
            }
        }
        config.setToolSwitches(switches);
        repository.save(config);
    }
}
