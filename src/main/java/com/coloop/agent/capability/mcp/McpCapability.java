package com.coloop.agent.capability.mcp;

import com.coloop.agent.core.tool.Tool;
import com.coloop.agent.runtime.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

public class McpCapability implements Tool {
    private static final Logger log = LoggerFactory.getLogger(McpCapability.class);
    private final AppConfig config;
    private final Map<String, McpClient> clients = new HashMap<>();
    private final List<Tool> tools = new ArrayList<>();
    private boolean initialized = false;

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
                log.warn("No MCP servers configured");
                return;
            }

            for (Map.Entry<String, McpServerConfig> entry : serversConfig.getMcpServers().entrySet()) {
                String serverName = entry.getKey();
                McpServerConfig serverConfig = entry.getValue();
                log.info("Connecting to MCP server: {}", serverName);

                try {
                    McpClient client = new McpClient(serverConfig);
                    client.initialize();

                    List<McpToolDefinition> mcpTools = client.listTools();
                    for (McpToolDefinition mcpTool : mcpTools) {
                        tools.add(new McpToolAdapter(mcpTool, client, serverName));
                        log.info("  - Tool: {}", mcpTool.getName());
                    }
                    clients.put(serverName, client);
                } catch (McpException e) {
                    log.error("Failed to connect to MCP server {}: {}", serverName, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to initialize MCP capability: {}", e.getMessage(), e);
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
                log.warn("MCP config file not found");
                return null;
            }

            return mapper.readValue(is, McpServersConfig.class);
        } catch (Exception e) {
            log.error("Failed to load MCP config: {}", e.getMessage());
            return null;
        }
    }

    public void close() {
        for (McpClient client : clients.values()) {
            client.close();
        }
    }
}