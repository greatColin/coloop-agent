package com.coloop.agent.capability.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class JsonRpcTest {

    @Test
    public void testJsonRpcRequestCreation() {
        JsonRpcRequest request = new JsonRpcRequest("tools/list", null);

        assertEquals("2.0", request.getJsonrpc());
        assertNotNull(request.getId());
        assertEquals("tools/list", request.getMethod());
        assertNull(request.getParams());
    }

    @Test
    public void testJsonRpcRequestWithParams() {
        Map<String, Object> params = Map.of("name", "test", "arguments", Map.of("key", "value"));
        JsonRpcRequest request = new JsonRpcRequest("tools/call", params);

        assertEquals("tools/call", request.getMethod());
        assertNotNull(request.getParams());
        assertEquals("test", request.getParams().get("name"));
    }

    @Test
    public void testJsonRpcRequestSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonRpcRequest request = new JsonRpcRequest("initialize", Map.of("key", "value"));

        String json = mapper.writeValueAsString(request);
        assertTrue(json.contains("\"jsonrpc\":\"2.0\""));
        assertTrue(json.contains("\"method\":\"initialize\""));
    }

    @Test
    public void testJsonRpcResponseParsing() throws Exception {
        String responseJson = """
            {
              "jsonrpc": "2.0",
              "id": "123",
              "result": {
                "tools": [
                  {
                    "name": "echo",
                    "description": "Echo tool",
                    "inputSchema": {"type": "object"}
                  }
                ]
              }
            }
            """;

        ObjectMapper mapper = new ObjectMapper();
        JsonRpcResponse response = mapper.readValue(responseJson, JsonRpcResponse.class);

        assertEquals("2.0", response.getJsonrpc());
        assertFalse(response.isError());
        assertNotNull(response.getResult());
    }

    @Test
    public void testJsonRpcErrorResponse() throws Exception {
        String errorJson = """
            {
              "jsonrpc": "2.0",
              "id": "123",
              "error": {
                "code": -32600,
                "message": "Invalid Request"
              }
            }
            """;

        ObjectMapper mapper = new ObjectMapper();
        JsonRpcResponse response = mapper.readValue(errorJson, JsonRpcResponse.class);

        assertTrue(response.isError());
        assertNotNull(response.getError());
        assertEquals(-32600, response.getError().getCode());
        assertEquals("Invalid Request", response.getError().getMessage());
    }
}