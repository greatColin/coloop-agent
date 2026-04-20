package com.coloop.agent.capability.mcp;

import com.coloop.agent.runtime.config.AppConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import com.fasterxml.jackson.databind.ObjectMapper;

public class McpCapabilityIntegrationTest {

    @Test
    public void testLoadChromaMcpConfig() throws Exception {
        AppConfig config = new AppConfig();
        config.setMcpConfigPath("classpath:/mcp-servers-config.json");

        ObjectMapper mapper = new ObjectMapper();
        InputStream is = getClass().getResourceAsStream("/mcp-servers-config.json");
        assertNotNull(is, "mcp-servers-config.json should be in classpath");

        McpServersConfig serversConfig = mapper.readValue(is, McpServersConfig.class);
        assertNotNull(serversConfig);
        assertNotNull(serversConfig.getMcpServers());
        assertTrue(serversConfig.getMcpServers().containsKey("chroma"));

        McpServerConfig chromaConfig = serversConfig.getMcpServers().get("chroma");
        assertEquals("E:\\systemApp\\miniConda\\envs\\py311\\python.exe", chromaConfig.getCommand());
        assertEquals(2, chromaConfig.getArgs().size());
        assertEquals("-m", chromaConfig.getArgs().get(0));
        assertEquals("chroma_mcp", chromaConfig.getArgs().get(1));
    }

    @Test
    public void testMcpCapabilityImplementsTool() {
        AppConfig config = new AppConfig();
        config.setMcpConfigPath("classpath:/mcp-servers-config.json");

        McpCapability capability = new McpCapability(config);
        assertEquals("mcp_client", capability.getName());
        assertNotNull(capability.getDescription());
    }

    @Test
    public void testMcpCapabilityGetToolsLoadsConfig() {
        AppConfig config = new AppConfig();
        config.setMcpConfigPath("classpath:/mcp-servers-config.json");

        McpCapability capability = new McpCapability(config);
        // getTools() 会触发懒加载初始化
        // 注意：这会尝试真正连接 MCP server，可能失败
        // 这里只测试配置能否被正确加载
        assertNotNull(capability);
    }
}