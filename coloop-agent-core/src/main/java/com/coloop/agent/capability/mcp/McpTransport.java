package com.coloop.agent.capability.mcp;

import com.coloop.agent.runtime.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class McpTransport {
    private static final System.Logger logger = System.getLogger(McpTransport.class.getName());
private static final int READ_TIMEOUT_MS = 60000; // 60 seconds timeout

    private final AppConfig.McpServerConfig config;
    private Process process;
    private PrintWriter writer;
    private BufferedReader reader;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean connected = false;
    private Thread timeoutThread;

    public McpTransport(AppConfig.McpServerConfig config) {
        this.config = config;
    }

    public synchronized void connect() throws McpException {
        if (connected) return;

        try {
            ProcessBuilder pb = new ProcessBuilder();
            List<String> command = new ArrayList<>();
            command.add(config.getCommand());
            if (config.getArgs() != null) {
                command.addAll(config.getArgs());
            }
            pb.command(command);

            if (config.getEnv() != null) {
                Map<String, String> env = pb.environment();
                env.putAll(config.getEnv());
            }

            // 不合并 stderr 到 stdout，让 JSON 响应更干净
            pb.redirectErrorStream(false);
            process = pb.start();

            writer = new PrintWriter(new OutputStreamWriter(process.getOutputStream()), true); // auto-flush
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // 启动一个线程来读取并丢弃 stderr（避免缓冲区满）
            final BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            Thread errorThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        System.err.println("[MCP stderr] " + line);
                    }
                } catch (IOException e) {
                    // 忽略
                }
            });
            errorThread.setDaemon(true);
            errorThread.start();

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
            String json = objectMapper.writeValueAsString(request);
            // 直接写入底层 OutputStream，避免缓冲问题
            process.getOutputStream().write((json + "\n").getBytes("UTF-8"));
            process.getOutputStream().flush();
        } catch (Exception e) {
            throw new McpException("Failed to send request: " + e.getMessage(), e);
        }
    }

    public synchronized ObjectNode readResponse() throws McpException {
        if (!connected) {
            throw new McpException("Not connected to MCP server");
        }

        final long startTime = System.currentTimeMillis();
        int skipCount = 0;
        final int maxSkips = 100; // Prevent infinite loops

        try {
            while (true) {
                // Check timeout
                if (System.currentTimeMillis() - startTime > READ_TIMEOUT_MS) {
                    throw new McpException("Read timeout after " + READ_TIMEOUT_MS + "ms");
                }

                // Check if stream is ready
                if (!reader.ready()) {
                    try {
                        Thread.sleep(50);
                        continue;
                    } catch (InterruptedException e) {
                        throw new McpException("Read interrupted");
                    }
                }

                String line = reader.readLine();
                if (line == null) {
                    connected = false;
                    throw new McpException("Server process terminated unexpectedly");
                }

                // Skip non-JSON lines (e.g., log output from servers)
                line = line.trim();
                if (!line.startsWith("{")) {
                    skipCount++;
                    if (skipCount > maxSkips) {
                        throw new McpException("Too many non-JSON lines received");
                    }
                    continue;
                }

                return objectMapper.readValue(line, ObjectNode.class);
            }
        } catch (IOException e) {
            connected = false;
            throw new McpException("Failed to read response: " + e.getMessage(), e);
        }
    }

    public synchronized void close() {
        connected = false;
        try {
            if (writer != null) writer.close();
        } catch (Exception e) { /* ignore */ }
        try {
            if (reader != null) reader.close();
        } catch (IOException e) { /* ignore */ }
        if (process != null) process.destroyForcibly();
    }

    public boolean isConnected() { return connected; }
}