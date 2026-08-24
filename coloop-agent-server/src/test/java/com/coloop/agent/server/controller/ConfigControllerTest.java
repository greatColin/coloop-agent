package com.coloop.agent.server.controller;

import com.coloop.agent.runtime.config.AppConfig;
import com.coloop.agent.server.config.SqliteConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigControllerTest {

    @TempDir
    Path tempDir;

    private ConfigController controller;

    @BeforeEach
    void setUp() {
        SqliteConfigRepository repo = new SqliteConfigRepository(tempDir.resolve("ctrl-test.db").toString());
        controller = new ConfigController(repo);
    }

    @Test
    void testGetReturnsSeedWhenNoStoredConfig() {
        Map<String, Object> body = controller.get();
        assertTrue((Boolean) body.get("fromSeed"));
        assertNotNull(body.get("models"));
    }

    @Test
    void testGetReturnsSavedConfig() {
        AppConfig cfg = validConfig();
        controller.save(cfg);

        Map<String, Object> body = controller.get();
        assertEquals(false, body.get("fromSeed"));
        assertTrue((Integer) body.get("version") >= 1);
        Map<?, ?> models = (Map<?, ?>) body.get("models");
        assertTrue(models.containsKey("minimax"));
    }

    @Test
    void testPutValidConfigSaves() {
        ResponseEntity<?> resp = controller.save(validConfig());
        assertEquals(200, resp.getStatusCodeValue());
        Map<?, ?> body = (Map<?, ?>) resp.getBody();
        assertEquals("saved", body.get("status"));
    }

    @Test
    void testPutRejectsMissingApiBase() {
        AppConfig cfg = validConfig();
        cfg.getModelConfig("minimax").setApiBase("");
        ResponseEntity<?> resp = controller.save(cfg);
        assertEquals(400, resp.getStatusCodeValue());
        Map<?, ?> body = (Map<?, ?>) resp.getBody();
        assertTrue(String.valueOf(body.get("details")).contains("apiBase"));
    }

    @Test
    void testPutRejectsUnknownDefaultModel() {
        AppConfig cfg = validConfig();
        cfg.setDefaultModel("not-exist");
        ResponseEntity<?> resp = controller.save(cfg);
        assertEquals(400, resp.getStatusCodeValue());
    }

    @Test
    void testPutRejectsMcpWithoutCommand() {
        AppConfig cfg = validConfig();
        AppConfig.McpServerConfig mcp = new AppConfig.McpServerConfig();
        mcp.setCommand("");
        mcp.setArgs(List.of("a"));
        Map<String, AppConfig.McpServerConfig> mcps = new HashMap<>();
        mcps.put("bad-mcp", mcp);
        cfg.setMcpServers(mcps);
        ResponseEntity<?> resp = controller.save(cfg);
        assertEquals(400, resp.getStatusCodeValue());
    }

    @Test
    void testDefaultReturnsSeedContent() {
        Map<String, Object> body = controller.defaultConfig();
        assertTrue(String.valueOf(body.get("content")).contains("defaultModel"));
    }

    private AppConfig validConfig() {
        AppConfig cfg = new AppConfig();
        cfg.setDefaultModel("minimax");
        AppConfig.ModelConfig mc = new AppConfig.ModelConfig();
        mc.setModel("MiniMax-M2.7");
        mc.setApiBase("https://api.minimaxi.com/v1");
        mc.setApiKey("sk-test");
        Map<String, AppConfig.ModelConfig> models = new HashMap<>();
        models.put("minimax", mc);
        cfg.setModels(models);
        AppConfig.McpServerConfig mcp = new AppConfig.McpServerConfig();
        mcp.setCommand("uvx");
        mcp.setArgs(List.of("minimax-coding-plan-mcp"));
        Map<String, AppConfig.McpServerConfig> mcps = new HashMap<>();
        mcps.put("MiniMax", mcp);
        cfg.setMcpServers(mcps);
        return cfg;
    }
}
