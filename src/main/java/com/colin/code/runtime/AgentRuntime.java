package com.colin.code.runtime;

import com.colin.code.core.agent.AgentLoop;

public class AgentRuntime {
    private final AgentLoop agentLoop;

    public AgentRuntime(AgentLoop agentLoop) {
        this.agentLoop = agentLoop;
    }

    public String chat(String input) {
        return agentLoop.chat(input);
    }
}
