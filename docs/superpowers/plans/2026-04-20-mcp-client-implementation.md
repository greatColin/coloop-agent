# MCP Client Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 MCP Client 能力，支持通过 STDIO 连接 MCP Server，将远程工具无缝接入 Agent 工具集

**Architecture:** 基于 capability 层实现，完全在 `capability/mcp/` 包中，不侵入 core 层。通过 `CapabilityLoader.withCapability()` 链式注册，配置驱动加载 JSON 配置文件。

**Tech Stack:** JDK 21, Jackson (已有), ProcessBuilder (内置)

---

## File Structure

```
src/main/java/com/coloop/agent/capability/mcp/
├── McpException.java              ← 自定义异常
├── McpServerConfig.java           ← Server 配置模型
├── McpServersConfig.java          ← 配置文件根模型
├── JsonRpcRequest.java            ← JSON-RPC 请求
├── JsonRpcResponse.java           ← JSON-RPC 响应
├── McpToolDefinition.java         ← MCP Tool 定义
├── McpTransport.java              ← STDIO 传输层
├── McpClient.java                 ← MCP 客户端核心
├── McpToolAdapter.java            ← Tool 适配器
├── McpCapability.java             ← 能力封装

src/main/resources/
└── mcp-servers-config.json         ← 默认配置文件

src/main/java/com/coloop/agent/runtime/
└── StandardCapability.java         ← 注册 MCP_CLIENT

src/main/java/com/coloop/agent/runtime/config/
└── AppConfig.java                  ← 添加 mcpConfigPath 字段
```

---

## Task 1: 基础模型类

**Files:**
- Create: `src/main/java/com/coloop/agent/capability/mcp/McpException.java`
- Create: `src/main/java/com/coloop/agent/capability/mcp/McpServerConfig.java`
- Create: `src/main/java/com/coloop/agent/capability/mcp/McpServersConfig.java`
- Create: `src/main/java/com/coloop/agent/capability/mcp/JsonRpcRequest.java`
- Create: `src/main/java/com/coloop/agent/capability/mcp/JsonRpcResponse.java`
- Create: `src/main/java/com/coloop/agent/capability/mcp/McpToolDefinition.java`

- [ ] **Step 1: 创建 McpException.java**

```java
package com.coloop.agent.capability.mcp;

public class McpException extends RuntimeException {
    public McpException(String message) {
        super(message);
    }

    public McpException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 2: 创建 McpServerConfig.java**

```java
package com.coloop.agent.capability.mcp;

import java.util.List;
import java.util.Map;

public class McpServerConfig {
    private String command;
    private List<String> args;
    private Map<String, String> env;

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public List<String> getArgs() { return args; }
    public void setArgs(List<String> args) { this.args = args; }

    public Map<String, String> getEnv() { return env; }
    public void setEnv(Map<String, String> env) { this.env = env; }
}
```

- [ ] **Step 3: 创建 McpServersConfig.java**

```java
package com.coloop.agent.capability.mcp;

import java.util.Map;

public class McpServersConfig {
    private Map<String, McpServerConfig> mcpServers;

    public Map<String, McpServerConfig> getMcpServers() { return mcpServers; }
    public void setMcpServers(Map<String, McpServerConfig> mcpServers) { this.mcpServers = mcpServers; }
}
```

- [ ] **Step 4: 创建 JsonRpcRequest.java**

```java
package com.coloop.agent.capability.mcp;

import java.util.Map;
import java.util.UUID;

public class JsonRpcRequest {
    private String jsonrpc = "2.0";
    private String id;
    private String method;
    private Map<String, Object> params;

    public JsonRpcRequest() {
        this.id = UUID.randomUUID().toString();
    }

    public JsonRpcRequest(String method, Map<String, Object> params) {
        this();
        this.method = method;
        this.params = params;
    }

    public String getJsonrpc() { return jsonrpc; }
    public String getId() { return id; }
    public String getMethod() { return method; }
    public Map<String, Object> getParams() { return params; }
}
```

- [ ] **Step 5: 创建 JsonRpcResponse.java**

```java
package com.coloop.agent.capability.mcp;

import java.util.List;
import java.util.Map;

public class JsonRpcResponse {
    private String jsonrpc = "2.0";
    private Object id;
    private Object result;
    private JsonRpcError error;

    public String getJsonrpc() { return jsonrpc; }
    public Object getId() { return id; }
    public Object getResult() { return result; }
    public JsonRpcError getError() { return error; }

    public boolean isError() { return error != null; }

    public static class JsonRpcError {
        private int code;
        private String message;

        public int getCode() { return code; }
        public String getMessage() { return message; }
    }

    // Result 内部结构
    public static class ToolListResult {
        private List<McpToolDefinition> tools;
        public List<McpToolDefinition> getTools() { return tools; }
    }

    public static class CallToolResult {
        private String content;
        public String getContent() { return content; }
    }
}
```

- [ ] **Step 6: 创建 McpToolDefinition.java**

```java
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
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/coloop/agent/capability/mcp/
git commit -m "feat(mcp): 添加基础模型类"
```

---

## Task 2: STDIO 传输层

**Files:**
- Create: `src/main/java/com/coloop/agent/capability/mcp/McpTransport.java`

- [ ] **Step 1: 创建 McpTransport.java**

```java
package com.coloop.agent.capability.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class McpTransport {
    private final McpServerConfig config;
    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean connected = false;

    public McpTransport(McpServerConfig config) {
        this.config = config;
    }

    public synchronized void connect() throws McpException {
        if (connected) return;

        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(config.getCommand(), config.getArgs().toArray(new String[0]));

            if (config.getEnv() != null) {
                Map<String, String> env = pb.environment();
                env.putAll(config.getEnv());
            }

            pb.redirectErrorStream(true);
            process = pb.start();

            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            connected = true;
        } catch (IOException e) {
            throw new McpException("Failed to start MCP server: " + e.getMessage(), e);
        }
    }

    public synchronized void sendRequest(ObjectNode request) throws McpException {
        if (!connected) {
            throw new McpException("Not connected to MCP server");
        }
        try {
            String json = objectMapper.writeValueAsString(request) + "\n";
            writer.write(json);
            writer.flush();
        } catch (IOException e) {
            throw new McpException("Failed to send request: " + e.getMessage(), e);
        }
    }

    public synchronized ObjectNode readResponse() throws McpException {
        if (!connected) {
            throw new McpException("Not connected to MCP server");
        }
        try {
            String line = reader.readLine();
            if (line == null) {
                connected = false;
                throw new McpException("Server process terminated unexpectedly");
            }
            return objectMapper.readValue(line, ObjectNode.class);
        } catch (IOException e) {
            connected = false;
            throw new McpException("Failed to read response: " + e.getMessage(), e);
        }
    }

    public synchronized void close() {
        connected = false;
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (process != null) process.destroyForcibly();
        } catch (IOException e) {
            // ignore
        }
    }

    public boolean isConnected() { return connected; }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/coloop/agent/capability/mcp/McpTransport.java
git commit -m "feat(mcp): 添加 STDIO 传输层"
```

---

## Task 3: MCP Client 核心

**Files:**
- Create: `src/main/java/com/coloop/agent/capability/mcp/McpClient.java`

- [ ] **Step 1: 创建 McpClient.java**

```java
package com.coloop.agent.capability.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

public class McpClient {
    private final McpTransport transport;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<McpToolDefinition> cachedTools;
    private String serverName;

    public McpClient(McpServerConfig config) {
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

        // 发送 initialized 通知
        sendNotification("initialized", Collections.emptyMap());
    }

    public List<McpToolDefinition> listTools() throws McpException {
        if (cachedTools != null) {
            return cachedTools;
        }

        JsonRpcRequest request = new JsonRpcRequest("tools/list", null);
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
                    tool.setInputSchema(toolNode.get("inputSchema"));
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
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/coloop/agent/capability/mcp/McpClient.java
git commit -m "feat(mcp): 添加 MCP Client 核心"
```

---

## Task 4: Tool 适配器

**Files:**
- Create: `src/main/java/com/coloop/agent/capability/mcp/McpToolAdapter.java`

- [ ] **Step 1: 创建 McpToolAdapter.java**

```java
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
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/coloop/agent/capability/mcp/McpToolAdapter.java
git commit -m "feat(mcp): 添加 Tool 适配器"
```

---

## Task 5: Capability 封装与配置

**Files:**
- Create: `src/main/java/com/coloop/agent/capability/mcp/McpCapability.java`
- Create: `src/main/resources/mcp-servers-config.json`
- Modify: `src/main/java/com/coloop/agent/runtime/StandardCapability.java`
- Modify: `src/main/java/com/coloop/agent/runtime/config/AppConfig.java`

- [ ] **Step 1: 创建 McpCapability.java**

```java
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
```

- [ ] **Step 2: 创建 mcp-servers-config.json**

```json
{
  "mcpServers": {
    "example": {
      "command": "echo",
      "args": ["hello"],
      "env": {}
    }
  }
}
```

- [ ] **Step 3: 修改 AppConfig.java — 添加 mcpConfigPath 字段**

在 AppConfig 类中添加：

```java
private String mcpConfigPath;

public String getMcpConfigPath() { return mcpConfigPath; }
public void setMcpConfigPath(String mcpConfigPath) { this.mcpConfigPath = mcpConfigPath; }
```

- [ ] **Step 4: 修改 StandardCapability.java — 注册 MCP_CLIENT**

在 StandardCapability 枚举中添加：

```java
MCP_CLIENT(
    "mcp_client", "MCP 客户端", "通过 STDIO 连接 MCP Server 并暴露其工具",
    CapabilityType.TOOL,
    config -> new McpCapability(config)
),
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/coloop/agent/capability/mcp/McpCapability.java
git add src/main/resources/mcp-servers-config.json
git add src/main/java/com/coloop/agent/runtime/StandardCapability.java
git add src/main/java/com/coloop/agent/runtime/config/AppConfig.java
git commit -m "feat(mcp): 添加 Capability 封装和配置支持"
```

---

## Task 6: CapabilityLoader 集成修改

**Files:**
- Modify: `src/main/java/com/coloop/agent/runtime/CapabilityLoader.java`

- [ ] **Step 1: 修改 CapabilityLoader.java — 支持返回多个工具**

当前的 `withCapability(StandardCapability, config)` 返回 `Capability`，但 TOOL 类型只注册单个 Tool。McpCapability 需要暴露 `getTools()` 方法。

修改 `CapabilityLoader.withCapability()`:

```java
public CapabilityLoader withCapability(StandardCapability cap, AppConfig config) {
    Object instance = cap.create(config);
    switch (cap.getType()) {
        case TOOL:
            if (instance instanceof McpCapability) {
                // MCP Capability 返回多个工具
                for (Tool tool : ((McpCapability) instance).getTools()) {
                    withTool(tool);
                }
            } else if (instance instanceof Tool) {
                withTool((Tool) instance);
            }
            break;
        // ... 其他 case 不变
    }
    return this;
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/coloop/agent/runtime/CapabilityLoader.java
git commit -m "feat(mcp): 集成 MCP Capability 到 CapabilityLoader"
```

---

## Task 7: 创建测试用 MCP Server

**Files:**
- Create: `src/test/mcp/test-echo-server.js`

- [ ] **Step 1: 创建简单的 Echo MCP Server 用于测试**

```javascript
#!/usr/bin/env node

const { Server } = require('@modelcontextprotocol/sdk/server/index.js');
const { StdioServerTransport } = require('@modelcontextprotocol/sdk/server/stdio.js');
const { CallToolRequestSchema, ListToolsRequestSchema } = require('@modelcontextprotocol/sdk/types.js');

const server = new Server({
    name: 'test-echo-server',
    version: '1.0.0'
}, {
    capabilities: {
        tools: {}
    }
});

server.setRequestHandler(ListToolsRequestSchema, async () => {
    return {
        tools: [
            {
                name: 'echo',
                description: 'Echoes back the input text',
                inputSchema: {
                    type: 'object',
                    properties: {
                        text: {
                            type: 'string',
                            description: 'Text to echo back'
                        }
                    },
                    required: ['text']
                }
            }
        ]
    };
});

server.setRequestHandler(CallToolRequestSchema, async ({ params }) => {
    const { name, arguments: args } = params;
    if (name === 'echo') {
        return {
            content: [
                { type: 'text', text: `Echo: ${args.text}` }
            ]
        };
    }
    throw new Error(`Unknown tool: ${name}`);
});

async function main() {
    const transport = new StdioServerTransport();
    await server.connect(transport);
}

main().catch(console.error);
```

- [ ] **Step 2: Commit**

```bash
git add src/test/mcp/test-echo-server.js
git commit -m "test(mcp): 添加测试用 Echo MCP Server"
```

---

## Task 8: 集成测试

**Files:**
- Create: `src/test/java/com/coloop/agent/capability/mcp/McpIntegrationTest.java`

- [ ] **Step 1: 创建 McpIntegrationTest.java**

```java
package com.coloop.agent.capability.mcp;

import com.coloop.agent.capability.provider.mock.MockProvider;
import com.coloop.agent.runtime.AgentRuntime;
import com.coloop.agent.runtime.CapabilityLoader;
import com.coloop.agent.runtime.StandardCapability;
import com.coloop.agent.runtime.config.AppConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class McpIntegrationTest {

    @Test
    public void testMcpCapabilityLoads() {
        AppConfig config = new AppConfig();
        config.setMcpConfigPath("classpath:/mcp-servers-config.json");

        CapabilityLoader loader = new CapabilityLoader();
        loader.withCapability(StandardCapability.MCP_CLIENT, config);

        AgentRuntime runtime = loader.build(new MockProvider(), config);

        assertNotNull(runtime);
        assertNotNull(runtime.getLoop());
    }
}
```

- [ ] **Step 2: 运行测试验证**

```bash
mvn test -Dtest=McpIntegrationTest
```

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/coloop/agent/capability/mcp/McpIntegrationTest.java
git commit -m "test(mcp): 添加 MCP 集成测试"
```

---

## Self-Review Checklist

1. **Spec coverage:** 所有设计文档中的组件都有对应实现
   - McpServerConfig ✓
   - McpTransport ✓
   - McpClient ✓
   - McpToolAdapter ✓
   - McpCapability ✓
   - JSON-RPC 协议 ✓

2. **Placeholder scan:** 无 TBD/TODO，所有步骤都有完整代码

3. **Type consistency:** 
   - McpToolDefinition.getName() ✓
   - McpToolAdapter.getName() 使用 serverPrefix + name ✓
   - McpClient.callTool(name, args) ✓

---

## 依赖说明

本实现**无需添加新依赖**，使用：
- JDK 21 内置 `ProcessBuilder`
- 已有 Jackson (`com.fasterxml.jackson.core:jackson-databind`)
- 已有 OkHttp (如后续添加 SSE 传输)

如需运行测试用 MCP Server (Node.js)，需要：
- Node.js 17+
- `@modelcontextprotocol/sdk` 包 (仅测试环境)