package com.coloop.agent.capability.mcp;

import com.coloop.agent.core.tool.Tool;
import com.coloop.agent.runtime.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;

public class McpCapability implements Tool {
    private final AppConfig config;
    private final Map<String, McpClient> clients = new HashMap<>();
    private final List<Tool> tools = new ArrayList<>();
    private boolean initialized = false;
    private final PrintStream log = System.out;

    public McpCapability(AppConfig config) {
        this.config = config;
    }

    @Override
    public String getName() {
        return "mcp_client";
    }

    @Override
    public String getDescription() {
        return "MCP Client - connects to MCP servers via STDIO and exposes their tools";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> props = new HashMap<>();
        props.put("type", "object");
        props.put("properties", new HashMap<>());
        return props;
    }

    @Override
    public String execute(Map<String, Object> params) {
        return "[Error: MCP capability is a container, not directly executable]";
    }

    public synchronized List<Tool> getTools() {
        if (!initialized) {
            initialize();
            initialized = true;
        }
        return tools;
    }

    private void initialize() {
        try {
            McpServersConfig serversConfig = loadConfig();
            if (serversConfig == null || serversConfig.getMcpServers() == null) {
                System.err.println("No MCP servers configured");
                return;
            }

            for (Map.Entry<String, McpServerConfig> entry : serversConfig.getMcpServers().entrySet()) {
                String serverName = entry.getKey();
                McpServerConfig serverConfig = entry.getValue();
                log.println("Connecting to MCP server: " + serverName);

                try {
                    McpClient client = new McpClient(serverConfig);
                    client.initialize();

                    List<McpToolDefinition> mcpTools = client.listTools();
                    for (McpToolDefinition mcpTool : mcpTools) {
                        tools.add(new McpToolAdapter(mcpTool, client, serverName));
                        log.println("  - Tool: " + mcpTool.getName());
                    }
                    clients.put(serverName, client);
                } catch (McpException e) {
                    System.err.println("Failed to connect to MCP server " + serverName + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize MCP capability: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private McpServersConfig loadConfig() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String configPath = config.getMcpConfigPath();
            InputStream is;

            if (configPath != null && !configPath.isEmpty()) {
                if (configPath.startsWith("classpath:")) {
                    is = getClass().getResourceAsStream("/" + configPath.substring(10));
                } else {
                    is = new java.io.FileInputStream(configPath);
                }
            } else {
                is = getClass().getResourceAsStream("/mcp-servers-config.json");
            }

            if (is == null) {
                System.err.println("MCP config file not found");
                return null;
            }

            return mapper.readValue(is, McpServersConfig.class);
        } catch (Exception e) {
            System.err.println("Failed to load MCP config: " + e.getMessage());
            return null;
        }
    }

    public void close() {
        for (McpClient client : clients.values()) {
            client.close();
        }
    }
}