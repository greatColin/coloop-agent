package com.coloop.agent.capability.mcp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class McpClientIntegrationTest {

    /**
     * 创建简单的 echo MCP server 用于测试
     */
    private File createEchoServerScript(Path tempDir) throws Exception {
        String script = """
            #!/usr/bin/env python3
            import sys
            import json

            def send_response(resp):
                sys.stdout.write(json.dumps(resp) + "\\n")
                sys.stdout.flush()

            def read_request():
                line = sys.stdin.readline()
                if not line:
                    return None
                return json.loads(line.strip())

            # Echo server implementation
            while True:
                req = read_request()
                if req is None:
                    break

                method = req.get("method", "")

                if method == "initialize":
                    send_response({
                        "jsonrpc": "2.0",
                        "id": req.get("id"),
                        "result": {
                            "protocolVersion": "2024-11-05",
                            "capabilities": {"tools": {}},
                            "serverInfo": {"name": "echo-server", "version": "1.0.0"}
                        }
                    })
                elif method == "initialized":
                    pass  # notification, no response
                elif method == "tools/list":
                    send_response({
                        "jsonrpc": "2.0",
                        "id": req.get("id"),
                        "result": {
                            "tools": [
                                {
                                    "name": "echo",
                                    "description": "Echoes back the input text",
                                    "inputSchema": {
                                        "type": "object",
                                        "properties": {
                                            "text": {
                                                "type": "string",
                                                "description": "Text to echo back"
                                            }
                                        },
                                        "required": ["text"]
                                    }
                                },
                                {
                                    "name": "add",
                                    "description": "Adds two numbers",
                                    "inputSchema": {
                                        "type": "object",
                                        "properties": {
                                            "a": {"type": "number"},
                                            "b": {"type": "number"}
                                        },
                                        "required": ["a", "b"]
                                    }
                                }
                            ]
                        }
                    })
                elif method == "tools/call":
                    name = req.get("params", {}).get("name")
                    args = req.get("params", {}).get("arguments", {})

                    if name == "echo":
                        text = args.get("text", "")
                        send_response({
                            "jsonrpc": "2.0",
                            "id": req.get("id"),
                            "result": {
                                "content": [{"type": "text", "text": f"Echo: {text}"}]
                            }
                        })
                    elif name == "add":
                        a = args.get("a", 0)
                        b = args.get("b", 0)
                        send_response({
                            "jsonrpc": "2.0",
                            "id": req.get("id"),
                            "result": {
                                "content": [{"type": "text", "text": str(a + b)}]
                            }
                        })
                    else:
                        send_response({
                            "jsonrpc": "2.0",
                            "id": req.get("id"),
                            "error": {"code": -32601, "message": f"Unknown tool: {name}"}
                        })
                else:
                    # Ignore other methods
                    pass
            """;

        File scriptFile = tempDir.resolve("echo_server.py").toFile();
        try (FileWriter fw = new FileWriter(scriptFile)) {
            fw.write(script);
        }
        return scriptFile;
    }

    @Test
    public void testMcpClientInitialize() throws Exception {
        Path tempDir = Files.createTempDirectory("mcp-test");
        File serverScript = createEchoServerScript(tempDir);

        try {
            McpServerConfig config = new McpServerConfig();
            config.setCommand("E:\\systemApp\\miniConda\\envs\\py311\\python.exe");
            config.setArgs(List.of(serverScript.getAbsolutePath()));
            config.setEnv(Map.of());

            McpClient client = new McpClient(config);

            try {
                client.initialize();
                assertEquals("echo-server", client.getServerName());
            } finally {
                client.close();
            }
        } finally {
            deleteDir(tempDir);
        }
    }

    @Test
    public void testMcpClientListTools() throws Exception {
        Path tempDir = Files.createTempDirectory("mcp-test");
        File serverScript = createEchoServerScript(tempDir);

        try {
            McpServerConfig config = new McpServerConfig();
            config.setCommand("E:\\systemApp\\miniConda\\envs\\py311\\python.exe");
            config.setArgs(List.of(serverScript.getAbsolutePath()));
            config.setEnv(Map.of());

            McpClient client = new McpClient(config);

            try {
                client.initialize();
                List<McpToolDefinition> tools = client.listTools();

                assertEquals(2, tools.size());

                // Check echo tool
                McpToolDefinition echoTool = tools.stream()
                    .filter(t -> t.getName().equals("echo"))
                    .findFirst()
                    .orElseThrow();
                assertEquals("Echoes back the input text", echoTool.getDescription());
                assertNotNull(echoTool.getInputSchema());

                // Check add tool
                McpToolDefinition addTool = tools.stream()
                    .filter(t -> t.getName().equals("add"))
                    .findFirst()
                    .orElseThrow();
                assertEquals("Adds two numbers", addTool.getDescription());
            } finally {
                client.close();
            }
        } finally {
            deleteDir(tempDir);
        }
    }

    @Test
    public void testMcpClientCallToolEcho() throws Exception {
        Path tempDir = Files.createTempDirectory("mcp-test");
        File serverScript = createEchoServerScript(tempDir);

        try {
            McpServerConfig config = new McpServerConfig();
            config.setCommand("E:\\systemApp\\miniConda\\envs\\py311\\python.exe");
            config.setArgs(List.of(serverScript.getAbsolutePath()));
            config.setEnv(Map.of());

            McpClient client = new McpClient(config);

            try {
                client.initialize();
                client.listTools();

                String result = client.callTool("echo", Map.of("text", "Hello MCP!"));

                assertEquals("Echo: Hello MCP!", result);
            } finally {
                client.close();
            }
        } finally {
            deleteDir(tempDir);
        }
    }

    @Test
    public void testMcpClientCallToolAdd() throws Exception {
        Path tempDir = Files.createTempDirectory("mcp-test");
        File serverScript = createEchoServerScript(tempDir);

        try {
            McpServerConfig config = new McpServerConfig();
            config.setCommand("E:\\systemApp\\miniConda\\envs\\py311\\python.exe");
            config.setArgs(List.of(serverScript.getAbsolutePath()));
            config.setEnv(Map.of());

            McpClient client = new McpClient(config);

            try {
                client.initialize();
                client.listTools();

                String result = client.callTool("add", Map.of("a", 5, "b", 3));

                assertEquals("8", result);
            } finally {
                client.close();
            }
        } finally {
            deleteDir(tempDir);
        }
    }

    @Test
    public void testMcpClientToolCaching() throws Exception {
        Path tempDir = Files.createTempDirectory("mcp-test");
        File serverScript = createEchoServerScript(tempDir);

        try {
            McpServerConfig config = new McpServerConfig();
            config.setCommand("E:\\systemApp\\miniConda\\envs\\py311\\python.exe");
            config.setArgs(List.of(serverScript.getAbsolutePath()));
            config.setEnv(Map.of());

            McpClient client = new McpClient(config);

            try {
                client.initialize();

                // First listTools call
                List<McpToolDefinition> tools1 = client.listTools();
                assertEquals(2, tools1.size());

                // Second listTools call should return cached result
                List<McpToolDefinition> tools2 = client.listTools();
                assertSame(tools1, tools2);  // Same instance (cached)
            } finally {
                client.close();
            }
        } finally {
            deleteDir(tempDir);
        }
    }

    @Test
    public void testMcpClientMultipleToolsCalls() throws Exception {
        Path tempDir = Files.createTempDirectory("mcp-test");
        File serverScript = createEchoServerScript(tempDir);

        try {
            McpServerConfig config = new McpServerConfig();
            config.setCommand("E:\\systemApp\\miniConda\\envs\\py311\\python.exe");
            config.setArgs(List.of(serverScript.getAbsolutePath()));
            config.setEnv(Map.of());

            McpClient client = new McpClient(config);

            try {
                client.initialize();
                client.listTools();

                // Call echo multiple times
                String result1 = client.callTool("echo", Map.of("text", "First"));
                String result2 = client.callTool("echo", Map.of("text", "Second"));
                String result3 = client.callTool("echo", Map.of("text", "Third"));

                assertEquals("Echo: First", result1);
                assertEquals("Echo: Second", result2);
                assertEquals("Echo: Third", result3);
            } finally {
                client.close();
            }
        } finally {
            deleteDir(tempDir);
        }
    }

    private void deleteDir(Path dir) {
        try {
            Files.walk(dir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        // ignore
                    }
                });
        } catch (IOException e) {
            // ignore
        }
    }
}