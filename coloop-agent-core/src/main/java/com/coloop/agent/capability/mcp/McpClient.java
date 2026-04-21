package com.coloop.agent.capability.mcp;

import com.coloop.agent.runtime.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

public class McpClient {
    private final McpTransport transport;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<McpToolDefinition> cachedTools;
    private String serverName;

    public McpClient(AppConfig.McpServerConfig config) {
        this.transport = new McpTransport(config);
    }

    public void initialize() throws McpException {
        transport.connect();

        Map<String, Object> params = new HashMap<>();
        params.put("protocolVersion", "2024-11-05");
        params.put("capabilities", Collections.emptyMap());
        params.put("clientInfo", Map.of(
            "name", "coloop-agent",
            "version", "1.0.0"
        ));

        JsonRpcRequest request = new JsonRpcRequest("initialize", params);
        ObjectNode response = sendRequest(request);

        if (response.has("error")) {
            throw new McpException("Initialize failed: " + response.get("error"));
        }

        ObjectNode result = (ObjectNode) response.get("result");
        this.serverName = result.has("serverInfo") ?
            result.get("serverInfo").get("name").asText() : "unknown";

// 发送 initialized 通知 (使用完整的 method name)
        sendNotification("notifications/initialized", Collections.emptyMap());
        // 不调用 drainPendingOutput() - 让服务器自然处理
    }

    public List<McpToolDefinition> listTools() throws McpException {
        if (cachedTools != null) {
            return cachedTools;
        }

// 对于 MCP SDK 的服务器，需要在发送请求前确保之前的通知已被处理
        // 发送 tools/list 请求 (使用空的 params 对象)
        JsonRpcRequest request = new JsonRpcRequest("tools/list", Collections.emptyMap());
        ObjectNode response = sendRequest(request);

        if (response.has("error")) {
            throw new McpException("List tools failed: " + response.get("error"));
        }

        cachedTools = parseTools((ObjectNode) response.get("result"));
        return cachedTools;
    }

    public String callTool(String name, Map<String, Object> args) throws McpException {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("arguments", args);

        JsonRpcRequest request = new JsonRpcRequest("tools/call", params);
        ObjectNode response = sendRequest(request);

        if (response.has("error")) {
            throw new McpException("Call tool failed: " + response.get("error"));
        }

        return extractContent(response);
    }

    private ObjectNode sendRequest(JsonRpcRequest request) throws McpException {
        ObjectNode node = objectMapper.valueToTree(request);
        transport.sendRequest(node);
        return transport.readResponse();
    }

    private void sendNotification(String method, Map<String, Object> params) throws McpException {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("jsonrpc", "2.0");
        node.put("method", method);
        if (params != null && !params.isEmpty()) {
            node.set("params", objectMapper.valueToTree(params));
        }
        transport.sendRequest(node);
    }

    private List<McpToolDefinition> parseTools(ObjectNode result) {
        List<McpToolDefinition> tools = new ArrayList<>();
        if (result.has("tools")) {
            ArrayNode toolsArray = (ArrayNode) result.get("tools");
            for (int i = 0; i < toolsArray.size(); i++) {
                ObjectNode toolNode = (ObjectNode) toolsArray.get(i);
                McpToolDefinition tool = new McpToolDefinition();
                tool.setName(toolNode.get("name").asText());
                tool.setDescription(toolNode.has("description") ?
                    toolNode.get("description").asText() : "");
                if (toolNode.has("inputSchema")) {
                    JsonNode inputSchemaNode = toolNode.get("inputSchema");
                    tool.setInputSchema(objectMapper.convertValue(inputSchemaNode, Map.class));
                }
                tools.add(tool);
            }
        }
        return tools;
    }

    private String extractContent(ObjectNode response) {
        ObjectNode result = (ObjectNode) response.get("result");
        if (result.has("content")) {
            ArrayNode content = (ArrayNode) result.get("content");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < content.size(); i++) {
                ObjectNode item = (ObjectNode) content.get(i);
                if (item.has("text")) {
                    sb.append(item.get("text").asText());
                }
            }
            return sb.toString();
        }
        return "";
    }

    public void close() {
        transport.close();
    }

    public String getServerName() { return serverName; }
}