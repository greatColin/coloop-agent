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