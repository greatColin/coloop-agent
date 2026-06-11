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