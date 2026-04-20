package com.coloop.agent.capability.mcp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class McpToolAdapterTest {

    @Test
    public void testToolAdapterCreation() {
        McpToolDefinition toolDef = new McpToolDefinition();
        toolDef.setName("echo");
        toolDef.setDescription("Echoes back input");
        toolDef.setInputSchema(Map.of(
            "type", "object",
            "properties", Map.of(
                "text", Map.of("type", "string")
            )
        ));

        // Create a mock McpClient (we'll test without actual connection)
        McpServerConfig config = new McpServerConfig();
        config.setCommand("echo");
        config.setArgs(java.util.List.of("test"));
        McpClient client = new McpClient(config);

        McpToolAdapter adapter = new McpToolAdapter(toolDef, client, "test");

        assertEquals("test_echo", adapter.getName());
        assertTrue(adapter.getDescription().contains("[MCP:"));
        assertTrue(adapter.getDescription().contains("Echoes back input"));
        assertNotNull(adapter.getParameters());
    }

    @Test
    public void testToolAdapterWithoutPrefix() {
        McpToolDefinition toolDef = new McpToolDefinition();
        toolDef.setName("echo");
        toolDef.setDescription("Echo");

        McpServerConfig config = new McpServerConfig();
        McpClient client = new McpClient(config);

        McpToolAdapter adapter = new McpToolAdapter(toolDef, client, "");

        assertEquals("echo", adapter.getName());
    }

    @Test
    public void testToolAdapterExecuteReturnsErrorWithoutConnection() {
        McpToolDefinition toolDef = new McpToolDefinition();
        toolDef.setName("echo");
        toolDef.setDescription("Echo");

        McpServerConfig config = new McpServerConfig();
        config.setCommand("nonexistent-command");
        config.setArgs(java.util.List.of());
        McpClient client = new McpClient(config);

        McpToolAdapter adapter = new McpToolAdapter(toolDef, client, "");

        // Should return error since no actual MCP server is running
        String result = adapter.execute(Map.of("text", "hello"));
        assertTrue(result.startsWith("[Error:"));
    }

    @Test
    public void testToolAdapterToSchema() {
        McpToolDefinition toolDef = new McpToolDefinition();
        toolDef.setName("echo");
        toolDef.setDescription("Echoes back");
        toolDef.setInputSchema(Map.of(
            "type", "object",
            "properties", Map.of("text", Map.of("type", "string"))
        ));

        McpClient client = new McpClient(new McpServerConfig());
        McpToolAdapter adapter = new McpToolAdapter(toolDef, client, "server");

        Map<String, Object> schema = adapter.toSchema();
        assertEquals("function", schema.get("type"));
        assertNotNull(schema.get("function"));
    }
}