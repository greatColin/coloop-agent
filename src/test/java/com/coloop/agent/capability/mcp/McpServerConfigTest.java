package com.coloop.agent.capability.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class McpServerConfigTest {

    @Test
    public void testParseServerConfig() throws Exception {
        String json = """
            {
              "command": "npx",
              "args": ["-y", "@modelcontextprotocol/server-filesystem", "/tmp"],
              "env": {"KEY": "value"}
            }
            """;

        ObjectMapper mapper = new ObjectMapper();
        McpServerConfig config = mapper.readValue(json, McpServerConfig.class);

        assertEquals("npx", config.getCommand());
        assertEquals(3, config.getArgs().size());
        assertEquals("-y", config.getArgs().get(0));
        assertEquals("@modelcontextprotocol/server-filesystem", config.getArgs().get(1));
        assertEquals("/tmp", config.getArgs().get(2));
        assertEquals("value", config.getEnv().get("KEY"));
    }

    @Test
    public void testParseServersConfig() throws Exception {
        String json = """
            {
              "mcpServers": {
                "filesystem": {
                  "command": "npx",
                  "args": ["-y", "@modelcontextprotocol/server-filesystem"],
                  "env": {}
                }
              }
            }
            """;

        ObjectMapper mapper = new ObjectMapper();
        McpServersConfig serversConfig = mapper.readValue(json, McpServersConfig.class);

        assertNotNull(serversConfig.getMcpServers());
        assertEquals(1, serversConfig.getMcpServers().size());
        assertTrue(serversConfig.getMcpServers().containsKey("filesystem"));
        assertEquals("npx", serversConfig.getMcpServers().get("filesystem").getCommand());
    }
}