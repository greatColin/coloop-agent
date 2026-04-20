package com.coloop.agent.capability.mcp;

import java.util.List;

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