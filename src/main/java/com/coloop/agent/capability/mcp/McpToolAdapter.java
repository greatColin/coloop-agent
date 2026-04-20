package com.coloop.agent.capability.mcp;

import com.coloop.agent.core.tool.BaseTool;

import java.util.Map;

public class McpToolAdapter extends BaseTool {
    private final McpToolDefinition definition;
    private final McpClient mcpClient;
    private final String serverPrefix;

    public McpToolAdapter(McpToolDefinition definition, McpClient mcpClient, String serverPrefix) {
        this.definition = definition;
        this.mcpClient = mcpClient;
        this.serverPrefix = serverPrefix;
    }

    @Override
    public String getName() {
        return serverPrefix.isEmpty() ? definition.getName() :
            serverPrefix + "_" + definition.getName();
    }

    @Override
    public String getDescription() {
        return "[MCP:" + mcpClient.getServerName() + "] " + definition.getDescription();
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> schema = definition.getInputSchema();
        if (schema == null) {
            schema = new java.util.HashMap<>();
            schema.put("type", "object");
            schema.put("properties", new java.util.HashMap<>());
        }
        return schema;
    }

    @Override
    public String execute(Map<String, Object> params) {
        try {
            return mcpClient.callTool(definition.getName(), params);
        } catch (McpException e) {
            return "[Error: " + e.getMessage() + "]";
        }
    }

    public McpToolDefinition getDefinition() { return definition; }
}