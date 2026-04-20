package com.coloop.agent.capability.prompt;

import com.coloop.agent.runtime.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentsMdPromptPluginTest {

    private AgentsMdPromptPlugin plugin;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        plugin = new AgentsMdPromptPlugin();
    }

    @Test
    void testGetName() {
        assertEquals("agents_md", plugin.getName());
    }

    @Test
    void testGetPriority() {
        assertEquals(20, plugin.getPriority());
    }

    @Test
    void testGenerateWithExistingAgentsMd() throws Exception {
        // 创建 AGENTS.md 文件
        String agentsMdContent = "# AGENTS.md - 项目约定\n\n## 编码规范\n使用 Java 17+ 特性";
        Path agentsMdPath = tempDir.resolve("AGENTS.md");
        Files.writeString(agentsMdPath, agentsMdContent);

        // 准备上下文
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("cwd", tempDir.toString());

        // 生成提示词
        AppConfig config = AppConfig.fromEnv();
        String result = plugin.generate(config, ctx);

        // 验证结果
        assertTrue(result.contains("## 项目约定（AGENTS.md）"));
        assertTrue(result.contains("# AGENTS.md - 项目约定"));
        assertTrue(result.contains("使用 Java 17+ 特性"));
    }

    @Test
    void testGenerateWithoutAgentsMd() {
        // 不创建 AGENTS.md 文件
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("cwd", tempDir.toString());

        AppConfig config = AppConfig.fromEnv();
        String result = plugin.generate(config, ctx);

        // 应该返回空字符串
        assertEquals("", result);
    }

    @Test
    void testGenerateWithChineseContent() throws Exception {
        // 测试中文内容
        String agentsMdContent = "# 项目约定\n\n## 编码规范\n- 遵循阿里巴巴 Java 编码规范\n- 类名使用 PascalCase";
        Path agentsMdPath = tempDir.resolve("AGENTS.md");
        Files.writeString(agentsMdPath, agentsMdContent);

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("cwd", tempDir.toString());

        AppConfig config = AppConfig.fromEnv();
        String result = plugin.generate(config, ctx);

        assertTrue(result.contains("阿里巴巴 Java 编码规范"));
        assertTrue(result.contains("PascalCase"));
    }

    @Test
    void testGenerateWithLargeFile() throws Exception {
        // 测试大文件
        StringBuilder largeContent = new StringBuilder("# AGENTS.md\n\n");
        for (int i = 0; i < 100; i++) {
            largeContent.append("## Section ").append(i).append("\n\nContent for section ").append(i).append("\n\n");
        }
        
        Path agentsMdPath = tempDir.resolve("AGENTS.md");
        Files.writeString(agentsMdPath, largeContent.toString());

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("cwd", tempDir.toString());

        AppConfig config = AppConfig.fromEnv();
        String result = plugin.generate(config, ctx);

        assertTrue(result.contains("Section 0"));
        assertTrue(result.contains("Section 99"));
    }
}
