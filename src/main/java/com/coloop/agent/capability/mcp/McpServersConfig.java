package com.coloop.agent.capability.mcp;

import java.util.Map;

public class McpServersConfig {
    private Map<String, McpServerConfig> mcpServers;

    public Map<String, McpServerConfig> getMcpServers() { return mcpServers; }
    public void setMcpServers(Map<String, McpServerConfig> mcpServers) { this.mcpServers = mcpServers; }
}