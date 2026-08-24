package com.coloop.agent.server.config;

import com.coloop.agent.runtime.config.AppConfig;
import com.coloop.agent.runtime.config.ConfigRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * 基于 SQLite 的配置仓库实现。配置以 JSON 形式存入单表 {@code app_config}，
 * 首次启动时若表为空，从 {@code coloop-agent-setting.json} 导入引导默认值。
 */
@Repository
public class SqliteConfigRepository implements ConfigRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String CONFIG_KEY = "main";
    private static final String SEED_RESOURCE = "coloop-agent-setting.json";

    private final String dbUrl;

    public SqliteConfigRepository(@Value("${coloop.config.db-path:./coloop-config.db}") String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
        initSchema();
    }

    /**
     * 初始化配置表结构。
     */
    public void initSchema() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS app_config (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "config_key TEXT UNIQUE NOT NULL," +
                    "config_json TEXT NOT NULL," +
                    "version INTEGER NOT NULL DEFAULT 1," +
                    "updated_at INTEGER NOT NULL)");
        } catch (SQLException e) {
            throw new IllegalStateException("初始化 SQLite 配置表失败: " + dbUrl, e);
        }
    }

    /**
     * 首次启动时导入配置文件引导默认值；已有持久化配置时跳过。
     */
    public void seedFromSetting() {
        if (hasStoredConfig()) {
            return;
        }
        try {
            AppConfig seed = AppConfig.fromSetting(SEED_RESOURCE);
            save(seed);
        } catch (IOException e) {
            System.err.println("[SqliteConfigRepository] 无法从配置文件导入引导配置: " + e.getMessage());
        }
    }

    @Override
    public Optional<AppConfig> load() {
        String sql = "SELECT config_json FROM app_config WHERE config_key = ? ORDER BY version DESC LIMIT 1";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, CONFIG_KEY);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    try {
                        return Optional.of(MAPPER.readValue(rs.getString("config_json"), AppConfig.class));
                    } catch (IOException e) {
                        System.err.println("[SqliteConfigRepository] 配置数据解析失败: " + e.getMessage());
                        return Optional.empty();
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            System.err.println("[SqliteConfigRepository] 读取配置失败: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void save(AppConfig config) {
        String sql = "INSERT INTO app_config (config_key, config_json, version, updated_at) VALUES (?, ?, 1, ?) " +
                "ON CONFLICT(config_key) DO UPDATE SET config_json = excluded.config_json, " +
                "version = app_config.version + 1, updated_at = excluded.updated_at";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, CONFIG_KEY);
            ps.setString(2, MAPPER.writeValueAsString(config));
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("保存配置失败: " + dbUrl, e);
        }
    }

    @Override
    public boolean hasStoredConfig() {
        String sql = "SELECT COUNT(*) FROM app_config WHERE config_key = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, CONFIG_KEY);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 当前持久化配置的版本号；无数据时返回 0。
     */
    public int getVersion() {
        String sql = "SELECT version FROM app_config WHERE config_key = ? ORDER BY version DESC LIMIT 1";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, CONFIG_KEY);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }
}
