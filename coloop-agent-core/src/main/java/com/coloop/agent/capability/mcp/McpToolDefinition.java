package com.coloop.agent.capability.mcp;

import java.util.Map;

public class McpToolDefinition {
    private String name;
    private String description;
    private Map<String, Object> inputSchema;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, Object> getInputSchema() { return inputSchema; }
    public void setInputSchema(Map<String, Object> inputSchema) { this.inputSchema = inputSchema; }
}