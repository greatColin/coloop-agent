package com.coloop.agent.capability.mcp;

import com.coloop.agent.core.tool.Tool;
import com.coloop.agent.runtime.config.AppConfig;

import java.util.*;

public class McpCapability implements Tool {
    private static final System.Logger logger = System.getLogger(McpCapability.class.getName());

    private final AppConfig config;
    private final Map<String, McpClient> clients = new HashMap<>();
    private final List<Tool> tools = new ArrayList<>();
    private boolean initialized = false;

    public McpCapability(AppConfig config) {
        this.config = config;
        logger.log(System.Logger.Level.INFO, "MCP Capability initialized");
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
        logger.log(System.Logger.Level.INFO, "Initializing MCP capability...");
        try {
            Map<String, AppConfig.McpServerConfig> servers = config.getMcpServers();
            if (servers == null || servers.isEmpty()) {
                logger.log(System.Logger.Level.WARNING, "No MCP servers configured");
                return;
            }

            for (Map.Entry<String, AppConfig.McpServerConfig> entry : servers.entrySet()) {
                connectServer(entry.getKey(), entry.getValue());
            }
            logger.log(System.Logger.Level.INFO, "MCP capability initialized with " + tools.size() + " tools total");
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Failed to initialize MCP capability: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void connectServer(String serverName, AppConfig.McpServerConfig serverConfig) {
        if (clients.containsKey(serverName)) {
            logger.log(System.Logger.Level.WARNING, "Server " + serverName + " already connected");
            return;
        }

        logger.log(System.Logger.Level.INFO, "Connecting to MCP server: " + serverName);
        try {
            McpClient client = new McpClient(serverConfig);
            client.initialize();

            List<McpToolDefinition> mcpTools = client.listTools();
            logger.log(System.Logger.Level.INFO, "Server " + serverName + " provided " + mcpTools.size() + " tools:");
            for (McpToolDefinition mcpTool : mcpTools) {
                tools.add(new McpToolAdapter(mcpTool, client, serverName));
                logger.log(System.Logger.Level.INFO, "  - Tool: " + mcpTool.getName());
            }
            clients.put(serverName, client);
        } catch (McpException e) {
            logger.log(System.Logger.Level.ERROR, "Failed to connect to MCP server " + serverName + ": " + e.getMessage());
        }
    }

    public synchronized void addServer(String name, String command, List<String> args, Map<String, String> env) {
        AppConfig.McpServerConfig serverConfig = new AppConfig.McpServerConfig();
        serverConfig.setCommand(command);
        serverConfig.setArgs(args != null ? args : new ArrayList<>());
        serverConfig.setEnv(env != null ? env : new HashMap<>());

        config.getMcpServers().put(name, serverConfig);
        connectServer(name, serverConfig);
    }

    public synchronized void addServer(String name, AppConfig.McpServerConfig serverConfig) {
        config.getMcpServers().put(name, serverConfig);
        connectServer(name, serverConfig);
    }

    public Map<String, McpClient> getClients() {
        return Collections.unmodifiableMap(clients);
    }

    public void close() {
        for (McpClient client : clients.values()) {
            client.close();
        }
    }
}
