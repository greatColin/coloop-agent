package com.coloop.agent.capability.mcp;

import com.coloop.agent.runtime.config.AppConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AppConfigMcpPathTest {

    @Test
    public void testMcpConfigPathGetterSetter() {
        AppConfig config = new AppConfig();

        assertNull(config.getMcpConfigPath());

        config.setMcpConfigPath("classpath:/mcp-servers-config.json");
        assertEquals("classpath:/mcp-servers-config.json", config.getMcpConfigPath());

        config.setMcpConfigPath("/path/to/custom-config.json");
        assertEquals("/path/to/custom-config.json", config.getMcpConfigPath());
    }
}