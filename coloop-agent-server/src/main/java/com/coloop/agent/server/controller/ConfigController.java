package com.coloop.agent.server.controller;

import com.coloop.agent.capability.CapabilityType;
import com.coloop.agent.runtime.StandardCapability;
import com.coloop.agent.runtime.config.AppConfig;
import com.coloop.agent.server.config.SqliteConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置管理 REST 接口：查询当前配置、保存配置、获取配置文件引导默认值。
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private static final String SEED_RESOURCE = "coloop-agent-setting.json";

    private final SqliteConfigRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConfigController(SqliteConfigRepository repository) {
        this.repository = repository;
    }

    /**
     * GET /api/config — 返回当前生效配置。无持久化数据时返回引导默认值并标记 fromSeed。
     */
    @GetMapping
    public Map<String, Object> get() {
        AppConfig config = repository.load().orElseGet(this::loadSeed);
        Map<String, Object> body = objectMapper.convertValue(config, new TypeReference<Map<String, Object>>() {});
        body.put("fromSeed", !repository.hasStoredConfig());
        body.put("version", repository.getVersion());
        if (config.getToolSwitches() == null || config.getToolSwitches().isEmpty()) {
            body.put("toolSwitches", defaultToolSwitches());
        }
        return body;
    }

    private Map<String, Boolean> defaultToolSwitches() {
        Map<String, Boolean> switches = new HashMap<>();
        for (StandardCapability cap : StandardCapability.values()) {
            if (cap.getType() == CapabilityType.TOOL || cap.getType() == CapabilityType.COMPOSITE) {
                switches.put(cap.getId(), true);
            }
        }
        return switches;
    }

    /**
     * GET /api/config/tools — 返回所有工具的 ID、名称、描述列表。
     */
    @GetMapping("/tools")
    public List<Map<String, String>> getTools() {
        List<Map<String, String>> tools = new ArrayList<>();
        for (StandardCapability cap : StandardCapability.values()) {
            if (cap.getType() == CapabilityType.TOOL || cap.getType() == CapabilityType.COMPOSITE) {
                Map<String, String> tool = new HashMap<>();
                tool.put("id", cap.getId());
                tool.put("name", cap.getName());
                tool.put("description", cap.getDescription());
                tools.add(tool);
            }
        }
        return tools;
    }

    /**
     * PUT /api/config — 校验并保存配置。
     */
    @PutMapping
    public ResponseEntity<?> save(@RequestBody AppConfig config) {
        List<String> errors = validate(config);
        if (!errors.isEmpty()) {
            Map<String, Object> body = new HashMap<>();
            body.put("error", "配置校验失败");
            body.put("details", errors);
            return ResponseEntity.badRequest().body(body);
        }
        repository.save(config);
        Map<String, Object> body = new HashMap<>();
        body.put("status", "saved");
        body.put("version", repository.getVersion());
        return ResponseEntity.ok(body);
    }

    /**
     * GET /api/config/default — 返回配置文件的原始内容（含注释），作为表单参考备注。
     */
    @GetMapping("/default")
    public Map<String, Object> defaultConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(SEED_RESOURCE)) {
            if (is == null) {
                return Map.of("content", "", "error", "seed 配置文件不存在: " + SEED_RESOURCE);
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return Map.of("content", content);
        } catch (IOException e) {
            return Map.of("content", "", "error", e.getMessage());
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "配置 JSON 格式错误");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private AppConfig loadSeed() {
        try {
            return AppConfig.fromSetting(SEED_RESOURCE);
        } catch (IOException e) {
            return new AppConfig();
        }
    }

    private List<String> validate(AppConfig config) {
        List<String> errors = new ArrayList<>();
        if (config.getDefaultModel() != null && !config.getDefaultModel().isEmpty()
                && !config.getModels().containsKey(config.getDefaultModel())) {
            errors.add("defaultModel 指向不存在的模型: " + config.getDefaultModel());
        }
        config.getModels().forEach((key, mc) -> {
            if (mc.getModel() == null || mc.getModel().isEmpty()) {
                errors.add("models." + key + ".model 不能为空");
            }
            if (mc.getApiBase() == null || mc.getApiBase().isEmpty()) {
                errors.add("models." + key + ".apiBase 不能为空");
            }
        });
        config.getMcpServers().forEach((key, mcp) -> {
            if (mcp.getCommand() == null || mcp.getCommand().isEmpty()) {
                errors.add("mcpServers." + key + ".command 不能为空");
            }
            if (mcp.getArgs() == null) {
                errors.add("mcpServers." + key + ".args 必须为数组");
            }
        });
        if (config.getMaxIterations() <= 0) {
            errors.add("maxIterations 必须为正整数");
        }
        if (config.getExecTimeoutSeconds() <= 0) {
            errors.add("execTimeoutSeconds 必须为正整数");
        }
        return errors;
    }
}
