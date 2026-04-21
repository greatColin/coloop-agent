package com.coloop.agent.capability.mcp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class McpToolDefinitionTest {

    @Test
    public void testToolDefinitionCreation() {
        McpToolDefinition tool = new McpToolDefinition();
        tool.setName("echo");
        tool.setDescription("Echoes back the input");
        tool.setInputSchema(Map.of("type", "object"));

        assertEquals("echo", tool.getName());
        assertEquals("Echoes back the input", tool.getDescription());
        assertNotNull(tool.getInputSchema());
        assertEquals("object", tool.getInputSchema().get("type"));
    }

    @Test
    public void testToolDefinitionWithParameters() {
        McpToolDefinition tool = new McpToolDefinition();
        tool.setName("read_file");
        tool.setDescription("Read a file");
        tool.setInputSchema(Map.of(
            "type", "object",
            "properties", Map.of(
                "path", Map.of("type", "string", "description", "File path")
            ),
            "required", java.util.List.of("path")
        ));

        assertEquals("read_file", tool.getName());
        assertNotNull(tool.getInputSchema());
        assertTrue(tool.getInputSchema().containsKey("properties"));
    }
}