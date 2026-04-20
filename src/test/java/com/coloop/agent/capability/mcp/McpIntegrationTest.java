package com.coloop.agent.capability.mcp;

import com.coloop.agent.capability.provider.mock.MockProvider;
import com.coloop.agent.runtime.AgentRuntime;
import com.coloop.agent.runtime.CapabilityLoader;
import com.coloop.agent.runtime.StandardCapability;
import com.coloop.agent.runtime.config.AppConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class McpIntegrationTest {

    @Test
    public void testMcpCapabilityLoads() {
        AppConfig config = new AppConfig();
        config.setMcpConfigPath("classpath:/mcp-servers-config.json");

        CapabilityLoader loader = new CapabilityLoader();
        loader.withCapability(StandardCapability.MCP_CLIENT, config);

        AgentRuntime runtime = loader.build(new MockProvider(), config);

        assertNotNull(runtime);
        assertNotNull(runtime.getLoop());
    }
}