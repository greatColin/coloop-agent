package com.coloop.agent.server.config;

import com.coloop.agent.runtime.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SqliteConfigRepositoryTest {

    @TempDir
    Path tempDir;

    private String dbPath() {
        return tempDir.resolve("test-config.db").toString();
    }

    @Test
    void testEmptyDbLoadReturnsEmpty() {
        SqliteConfigRepository repo = new SqliteConfigRepository(dbPath());
        assertFalse(repo.hasStoredConfig());
        assertTrue(repo.load().isEmpty());
    }

    @Test
    void testSeedImportsConfigFileDefaults() {
        SqliteConfigRepository repo = new SqliteConfigRepository(dbPath());
        repo.seedFromSetting();
        assertTrue(repo.hasStoredConfig());
        Optional<AppConfig> loaded = repo.load();
        assertTrue(loaded.isPresent());
        assertFalse(loaded.get().getModels().isEmpty());
        assertFalse(loaded.get().getDefaultModel() == null);
    }

    @Test
    void testSeedSkipsWhenConfigExists() {
        SqliteConfigRepository repo = new SqliteConfigRepository(dbPath());
        AppConfig custom = new AppConfig();
        custom.setDefaultModel("custom");
        repo.save(custom);

        repo.seedFromSetting();
        Optional<AppConfig> loaded = repo.load();
        assertTrue(loaded.isPresent());
        assertEquals("custom", loaded.get().getDefaultModel());
    }

    @Test
    void testSaveOverwritesExistingConfig() {
        SqliteConfigRepository repo = new SqliteConfigRepository(dbPath());

        AppConfig first = new AppConfig();
        first.setDefaultModel("model-a");
        Map<String, AppConfig.ModelConfig> models = new HashMap<>();
        AppConfig.ModelConfig mc = new AppConfig.ModelConfig();
        mc.setModel("ModelA");
        mc.setApiBase("https://a.example.com");
        models.put("model-a", mc);
        first.setModels(models);
        repo.save(first);

        AppConfig second = new AppConfig();
        second.setDefaultModel("model-b");
        Map<String, AppConfig.ModelConfig> models2 = new HashMap<>();
        AppConfig.ModelConfig mc2 = new AppConfig.ModelConfig();
        mc2.setModel("ModelB");
        mc2.setApiBase("https://b.example.com");
        models2.put("model-b", mc2);
        second.setModels(models2);
        repo.save(second);

        Optional<AppConfig> loaded = repo.load();
        assertTrue(loaded.isPresent());
        assertEquals("model-b", loaded.get().getDefaultModel());
        assertNull(loaded.get().getModelConfig("model-a"));
        assertNotNull(loaded.get().getModelConfig("model-b"));
    }

    @Test
    void testVoiceConfigPreservedOnRoundTrip() {
        SqliteConfigRepository repo = new SqliteConfigRepository(dbPath());
        AppConfig config = new AppConfig();
        config.setDefaultModel("minimax");
        Map<String, Object> voice = new HashMap<>();
        voice.put("language", "zh");
        config.setVoice(voice);
        repo.save(config);

        Optional<AppConfig> loaded = repo.load();
        assertTrue(loaded.isPresent());
        assertEquals("zh", loaded.get().getVoice().get("language"));
    }

    @Test
    void testCorruptedJsonFallsBackToEmpty() throws Exception {
        SqliteConfigRepository repo = new SqliteConfigRepository(dbPath());
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath());
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO app_config (config_key, config_json, version, updated_at) " +
                    "VALUES ('main', '{not-valid-json', 1, 1)");
        }
        assertTrue(repo.load().isEmpty());
    }
}
