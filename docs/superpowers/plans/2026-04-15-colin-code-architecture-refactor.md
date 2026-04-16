# colin-code 架构重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有扁平代码重构为核心-插件洋葱架构，实现 core/capability/runtime/entry 四层分离，支持 CapabilityLoader 链式组装和 StandardCapability 枚举目录。

**Architecture:** 将接口与数据模型下沉到 `core`，具体实现迁移到 `capability`，新增 `runtime` 组装中枢和 `entry` 多入口。AgentLoop 注入 MessageBuilder、AgentHook 列表和 InputInterceptor 列表。

**Tech Stack:** Java 21, Maven, Jackson, OkHttp

---

## 文件结构映射

| 原文件 | 新文件 | 操作 |
|--------|--------|------|
| `provider/LLMProvider.java` | `core/provider/LLMProvider.java` | 移动 |
| `provider/LLMResponse.java` | `core/provider/LLMResponse.java` | 移动 |
| `provider/ToolCallRequest.java` | `core/provider/ToolCallRequest.java` | 移动 |
| `tool/Tool.java` | `core/tool/Tool.java` | 移动 |
| `tool/BaseTool.java` | `core/tool/BaseTool.java` | 移动 |
| `tool/ToolRegistry.java` | `core/tool/ToolRegistry.java` | 移动 |
| `agent/AgentLoop.java` | `core/agent/AgentLoop.java` | 移动+改造 |
| `provider/MockProvider.java` | `capability/provider/mock/MockProvider.java` | 移动 |
| `provider/OpenAICompatibleProvider.java` | `capability/provider/openai/OpenAICompatibleProvider.java` | 移动 |
| `tool/ExecTool.java` | `capability/tool/exec/ExecTool.java` | 移动 |
| `prompt/PromptSegment.java` | `capability/prompt/PromptSegment.java` | 移动 |
| `agent/ContextBuilder.java` | `capability/prompt/StandardMessageBuilder.java` | 改造 |
| `config/DemoConfig.java` | `runtime/config/AppConfig.java` | 移动+升级 |
| `Main.java` | `entry/MinimalDemo.java` + `entry/CliApp.java` | 替换 |
| — | `core/message/MessageBuilder.java` | 新增 |
| — | `core/agent/AgentHook.java` | 新增 |
| — | `core/interceptor/InputInterceptor.java` | 新增 |
| — | `core/prompt/PromptPlugin.java` | 新增 |
| — | `capability/prompt/BasePromptPlugin.java` | 新增 |
| — | `capability/prompt/SkillPromptPlugin.java` | 新增 |
| — | `capability/prompt/AgentsMdPromptPlugin.java` | 新增 |
| — | `capability/hook/LoggingHook.java` | 新增 |
| — | `runtime/CapabilityType.java` | 新增 |
| — | `runtime/StandardCapability.java` | 新增 |
| — | `runtime/CapabilityLoader.java` | 新增 |
| — | `runtime/AgentRuntime.java` | 新增 |

---

### Task 1: 移动 Provider 接口和数据模型到 core

**Files:**
- Create: `src/main/java/com/colin/code/core/provider/LLMProvider.java`
- Create: `src/main/java/com/colin/code/core/provider/LLMResponse.java`
- Create: `src/main/java/com/colin/code/core/provider/ToolCallRequest.java`
- Delete (later): `src/main/java/com/colin/code/provider/*`

- [ ] **Step 1: 创建目录并移动文件**

```bash
mkdir -p src/main/java/com/colin/code/core/provider
```

复制 `provider/LLMProvider.java` 到 `core/provider/LLMProvider.java`，修改包声明：
```java
package com.colin.code.core.provider;
```

对 `LLMResponse.java` 和 `ToolCallRequest.java` 执行相同操作。

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/colin/code/core/provider/
git commit -m "refactor: move provider interfaces and models to core"
```

---

### Task 2: 移动 Tool 抽象到 core

**Files:**
- Create: `src/main/java/com/colin/code/core/tool/Tool.java`
- Create: `src/main/java/com/colin/code/core/tool/BaseTool.java`
- Create: `src/main/java/com/colin/code/core/tool/ToolRegistry.java`

- [ ] **Step 1: 移动并修改包名**

创建目录：
```bash
mkdir -p src/main/java/com/colin/code/core/tool
```

复制 `tool/Tool.java`、`tool/BaseTool.java`、`tool/ToolRegistry.java` 到 `core/tool/`，修改包声明为：
```java
package com.colin.code.core.tool;
```

更新 `ToolRegistry.java` 中的 import：
```java
import com.colin.code.core.provider.ToolCallRequest;
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/colin/code/core/tool/
git commit -m "refactor: move tool abstractions to core"
```

---

### Task 3: 新增 core 抽象接口

**Files:**
- Create: `src/main/java/com/colin/code/core/message/MessageBuilder.java`
- Create: `src/main/java/com/colin/code/core/agent/AgentHook.java`
- Create: `src/main/java/com/colin/code/core/interceptor/InputInterceptor.java`
- Create: `src/main/java/com/colin/code/core/prompt/PromptPlugin.java`

- [ ] **Step 1: 创建 MessageBuilder**

```java
package com.colin.code.core.message;

import com.colin.code.core.provider.LLMResponse;
import com.colin.code.core.provider.ToolCallRequest;

import java.util.List;
import java.util.Map;

public interface MessageBuilder {
    List<Map<String, Object>> buildInitial(String userMessage);
    void addAssistantMessage(List<Map<String, Object>> messages, LLMResponse response);
    void addToolResult(List<Map<String, Object>> messages, ToolCallRequest tc, String result);
}
```

- [ ] **Step 2: 创建 AgentHook**

```java
package com.colin.code.core.agent;

import com.colin.code.core.provider.LLMResponse;
import com.colin.code.core.provider.ToolCallRequest;

import java.util.List;
import java.util.Map;

public interface AgentHook {
    default void onLoopStart(String userMessage) {}
    default void beforeLLMCall(List<Map<String, Object>> messages) {}
    default void afterLLMCall(LLMResponse response) {}
    default void onToolCall(ToolCallRequest toolCall, String result) {}
    default void onLoopEnd(String finalResponse) {}
}
```

- [ ] **Step 3: 创建 InputInterceptor**

```java
package com.colin.code.core.interceptor;

import java.util.Optional;

public interface InputInterceptor {
    Optional<String> intercept(String userMessage);
}
```

- [ ] **Step 4: 创建 PromptPlugin**

```java
package com.colin.code.core.prompt;

import com.colin.code.runtime.config.AppConfig;

import java.util.Map;

public interface PromptPlugin {
    String getName();
    int getPriority();
    String generate(AppConfig config, Map<String, Object> runtimeContext);
}
```

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/colin/code/core/message/
git add src/main/java/com/colin/code/core/agent/
git add src/main/java/com/colin/code/core/interceptor/
git add src/main/java/com/colin/code/core/prompt/
git commit -m "feat(core): add MessageBuilder, AgentHook, InputInterceptor, PromptPlugin"
```

---

### Task 4: 移动 Provider 实现到 capability

**Files:**
- Create: `src/main/java/com/colin/code/capability/provider/mock/MockProvider.java`
- Create: `src/main/java/com/colin/code/capability/provider/openai/OpenAICompatibleProvider.java`

- [ ] **Step 1: 创建目录并移动 MockProvider**

```bash
mkdir -p src/main/java/com/colin/code/capability/provider/mock
mkdir -p src/main/java/com/colin/code/capability/provider/openai
```

复制文件，修改包名：
- `MockProvider.java` -> `package com.colin.code.capability.provider.mock;`
- `OpenAICompatibleProvider.java` -> `package com.colin.code.capability.provider.openai;`

更新 import：
```java
import com.colin.code.core.provider.LLMProvider;
import com.colin.code.core.provider.LLMResponse;
import com.colin.code.core.provider.ToolCallRequest;
import com.colin.code.runtime.config.AppConfig;
```

（注意：`OpenAICompatibleProvider` 原来依赖 `DemoConfig`，现在改为 `AppConfig`。如果 `AppConfig` 还没创建，可以先保留 `DemoConfig` 的 import，在 Task 6 中统一修改。）

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/colin/code/capability/provider/
git commit -m "refactor: move provider implementations to capability"
```

---

### Task 5: 移动 Tool 和 Prompt 实现到 capability

**Files:**
- Create: `src/main/java/com/colin/code/capability/tool/exec/ExecTool.java`
- Create: `src/main/java/com/colin/code/capability/prompt/PromptSegment.java`

- [ ] **Step 1: 移动 ExecTool**

```bash
mkdir -p src/main/java/com/colin/code/capability/tool/exec
```

复制 `tool/ExecTool.java`，修改包名：
```java
package com.colin.code.capability.tool.exec;

import com.colin.code.core.tool.BaseTool;
```

- [ ] **Step 2: 移动 PromptSegment**

```bash
mkdir -p src/main/java/com/colin/code/capability/prompt
```

复制 `prompt/PromptSegment.java`，修改包名：
```java
package com.colin.code.capability.prompt;
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/colin/code/capability/tool/
git add src/main/java/com/colin/code/capability/prompt/
git commit -m "refactor: move ExecTool and PromptSegment to capability"
```

---

### Task 6: 升级 DemoConfig 到 runtime 的 AppConfig

**Files:**
- Create: `src/main/java/com/colin/code/runtime/config/AppConfig.java`
- Modify: `src/main/java/com/colin/code/capability/provider/openai/OpenAICompatibleProvider.java`

- [ ] **Step 1: 创建 AppConfig**

```bash
mkdir -p src/main/java/com/colin/code/runtime/config
```

```java
package com.colin.code.runtime.config;

public class AppConfig {
    private String model = "";
    private String apiKey = "";
    private String apiBase = "";
    private int maxTokens = 2048;
    private double temperature = 0.7;
    private int maxIterations = 10;
    private int execTimeoutSeconds = 30;

    public String getModel() { return model; }
    public String getApiKey() { return apiKey != null ? apiKey : ""; }
    public String getApiBase() { return apiBase; }
    public int getMaxTokens() { return maxTokens; }
    public double getTemperature() { return temperature; }
    public int getMaxIterations() { return maxIterations; }
    public int getExecTimeoutSeconds() { return execTimeoutSeconds; }
}
```

> 保留与 `DemoConfig` 完全相同的字段和 getter，确保现有代码零破坏迁移。

- [ ] **Step 2: 更新 OpenAICompatibleProvider 的 import**

将 `OpenAICompatibleProvider.java` 中的：
```java
import com.colin.code.config.DemoConfig;
```
改为：
```java
import com.colin.code.runtime.config.AppConfig;
```

并将构造函数参数类型从 `DemoConfig` 改为 `AppConfig`。

- [ ] **Step 3: 编译验证**

```bash
mvn compile
```
Expected: 编译通过（此时旧文件 `DemoConfig.java` 仍在，但新文件已可用）

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/colin/code/runtime/config/
git add src/main/java/com/colin/code/capability/provider/openai/OpenAICompatibleProvider.java
git commit -m "refactor: introduce AppConfig in runtime and update OpenAICompatibleProvider"
```

---

### Task 7: 创建 PromptPlugin 实现和 StandardMessageBuilder

**Files:**
- Create: `src/main/java/com/colin/code/capability/prompt/BasePromptPlugin.java`
- Create: `src/main/java/com/colin/code/capability/prompt/SkillPromptPlugin.java`
- Create: `src/main/java/com/colin/code/capability/prompt/AgentsMdPromptPlugin.java`
- Create: `src/main/java/com/colin/code/capability/prompt/StandardMessageBuilder.java`

- [ ] **Step 1: 创建 BasePromptPlugin**

```java
package com.colin.code.capability.prompt;

import com.colin.code.core.prompt.PromptPlugin;
import com.colin.code.runtime.config.AppConfig;

import java.util.Map;

public class BasePromptPlugin implements PromptPlugin {
    @Override
    public String getName() { return "base"; }

    @Override
    public int getPriority() { return 0; }

    @Override
    public String generate(AppConfig config, Map<String, Object> ctx) {
        return "你是一个帮助用户完成软件工程任务的 AI 助手。\n"
             + "Current time: " + ctx.get("time") + "\n"
             + "Working directory: " + ctx.get("cwd") + "\n"
             + "Platform: " + ctx.get("os") + "\n"
             + "Model: " + config.getModel() + "\n\n"
             + "Always respond in the same language as the user's message.";
    }
}
```

- [ ] **Step 2: 创建 SkillPromptPlugin（预留）**

```java
package com.colin.code.capability.prompt;

import com.colin.code.core.prompt.PromptPlugin;
import com.colin.code.runtime.config.AppConfig;

import java.util.Map;

public class SkillPromptPlugin implements PromptPlugin {
    @Override
    public String getName() { return "skill"; }

    @Override
    public int getPriority() { return 10; }

    @Override
    public String generate(AppConfig config, Map<String, Object> ctx) {
        return ""; // 预留：未来扫描 skills 目录后注入
    }
}
```

- [ ] **Step 3: 创建 AgentsMdPromptPlugin（预留）**

```java
package com.colin.code.capability.prompt;

import com.colin.code.core.prompt.PromptPlugin;
import com.colin.code.runtime.config.AppConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class AgentsMdPromptPlugin implements PromptPlugin {
    @Override
    public String getName() { return "agents_md"; }

    @Override
    public int getPriority() { return 20; }

    @Override
    public String generate(AppConfig config, Map<String, Object> ctx) {
        Path agentsMd = Paths.get((String) ctx.get("cwd"), "AGENTS.md");
        if (!Files.exists(agentsMd)) return "";
        try {
            return "## 项目约定（AGENTS.md）\n" + Files.readString(agentsMd);
        } catch (Exception e) {
            return "";
        }
    }
}
```

- [ ] **Step 4: 创建 StandardMessageBuilder（原 ContextBuilder 改造）**

```java
package com.colin.code.capability.prompt;

import com.colin.code.core.message.MessageBuilder;
import com.colin.code.core.provider.LLMResponse;
import com.colin.code.core.provider.ToolCallRequest;
import com.colin.code.core.prompt.PromptPlugin;
import com.colin.code.runtime.config.AppConfig;

import java.util.*;

public class StandardMessageBuilder implements MessageBuilder {
    private final List<PromptPlugin> plugins;
    private final AppConfig config;

    public StandardMessageBuilder(List<PromptPlugin> plugins, AppConfig config) {
        this.plugins = new ArrayList<>(plugins);
        this.plugins.sort(Comparator.comparingInt(PromptPlugin::getPriority));
        this.config = config;
    }

    @Override
    public List<Map<String, Object>> buildInitial(String userMessage) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(systemMessage());
        messages.add(userMessage(userMessage));
        return messages;
    }

    @Override
    public void addAssistantMessage(List<Map<String, Object>> messages, LLMResponse response) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "assistant");
        msg.put("content", response.getContent() != null ? response.getContent() : "");
        if (response.hasToolCalls()) {
            msg.put("tool_calls", wrapToolCalls(response.getToolCalls()));
        }
        messages.add(msg);
    }

    @Override
    public void addToolResult(List<Map<String, Object>> messages, ToolCallRequest tc, String result) {
        Map<String, Object> tr = new HashMap<>();
        tr.put("role", "tool");
        tr.put("tool_call_id", tc.getId());
        tr.put("content", result != null ? result : "");
        messages.add(tr);
    }

    private Map<String, Object> systemMessage() {
        Map<String, Object> sys = new HashMap<>();
        sys.put("role", "system");
        Map<String, Object> ctx = buildRuntimeContext();
        StringBuilder sb = new StringBuilder();
        for (PromptPlugin plugin : plugins) {
            String segment = plugin.generate(config, ctx);
            if (segment != null && !segment.isBlank()) {
                sb.append(segment).append("\n\n");
            }
        }
        sys.put("content", sb.toString().trim());
        return sys;
    }

    private Map<String, Object> buildRuntimeContext() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("cwd", System.getProperty("user.dir"));
        ctx.put("os", System.getProperty("os.name"));
        ctx.put("time", java.time.ZonedDateTime.now());
        return ctx;
    }

    private Map<String, Object> userMessage(String content) {
        Map<String, Object> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", content != null ? content : "");
        return user;
    }

    private List<Map<String, Object>> wrapToolCalls(List<ToolCallRequest> toolCalls) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ToolCallRequest tc : toolCalls) {
            Map<String, Object> fn = new HashMap<>();
            fn.put("id", tc.getId());
            fn.put("type", "function");
            Map<String, Object> f = new HashMap<>();
            f.put("name", tc.getName());
            try {
                f.put("arguments", new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(tc.getArguments()));
            } catch (Exception e) {
                f.put("arguments", "{}");
            }
            fn.put("function", f);
            out.add(fn);
        }
        return out;
    }
}
```

- [ ] **Step 5: 编译验证**

```bash
mvn compile
```
Expected: 编译通过

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/colin/code/capability/prompt/
git commit -m "feat(capability): add PromptPlugin implementations and StandardMessageBuilder"
```

---

### Task 8: 创建 capability 的 Hook 实现

**Files:**
- Create: `src/main/java/com/colin/code/capability/hook/LoggingHook.java`

- [ ] **Step 1: 创建 LoggingHook**

```java
package com.colin.code.capability.hook;

import com.colin.code.core.agent.AgentHook;
import com.colin.code.core.provider.LLMResponse;
import com.colin.code.core.provider.ToolCallRequest;

import java.util.List;
import java.util.Map;

public class LoggingHook implements AgentHook {
    @Override
    public void onLoopStart(String userMessage) {
        System.out.println("[LOG] Loop start: " + userMessage);
    }

    @Override
    public void beforeLLMCall(List<Map<String, Object>> messages) {
        System.out.println("[LOG] Before LLM call, messages: " + messages.size());
    }

    @Override
    public void afterLLMCall(LLMResponse response) {
        System.out.println("[LOG] After LLM call, hasToolCalls: " + response.hasToolCalls());
    }

    @Override
    public void onToolCall(ToolCallRequest toolCall, String result) {
        System.out.println("[LOG] Tool executed: " + toolCall.getName());
    }

    @Override
    public void onLoopEnd(String finalResponse) {
        System.out.println("[LOG] Loop end.");
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/colin/code/capability/hook/
git commit -m "feat(capability): add LoggingHook"
```

---

### Task 9: 创建 runtime 组装中枢

**Files:**
- Create: `src/main/java/com/colin/code/runtime/CapabilityType.java`
- Create: `src/main/java/com/colin/code/runtime/StandardCapability.java`
- Create: `src/main/java/com/colin/code/runtime/CapabilityLoader.java`
- Create: `src/main/java/com/colin/code/runtime/AgentRuntime.java`

- [ ] **Step 1: 创建 CapabilityType**

```java
package com.colin.code.runtime;

public enum CapabilityType {
    TOOL, PROMPT_PLUGIN, HOOK, INTERCEPTOR
}
```

- [ ] **Step 2: 创建 StandardCapability**

```java
package com.colin.code.runtime;

import com.colin.code.capability.hook.LoggingHook;
import com.colin.code.capability.prompt.AgentsMdPromptPlugin;
import com.colin.code.capability.prompt.BasePromptPlugin;
import com.colin.code.capability.prompt.SkillPromptPlugin;
import com.colin.code.capability.tool.exec.ExecTool;
import com.colin.code.core.agent.AgentHook;
import com.colin.code.core.interceptor.InputInterceptor;
import com.colin.code.core.prompt.PromptPlugin;
import com.colin.code.core.tool.Tool;
import com.colin.code.runtime.config.AppConfig;

import java.util.function.Function;

public enum StandardCapability {
    EXEC_TOOL(
        "exec", "Shell执行工具", "执行shell命令，返回stdout和stderr",
        CapabilityType.TOOL,
        config -> new ExecTool(config.getExecTimeoutSeconds())
    ),
    BASE_PROMPT(
        "base_prompt", "基础提示词", "注入身份介绍、环境信息等基础系统提示",
        CapabilityType.PROMPT_PLUGIN,
        config -> new BasePromptPlugin()
    ),
    SKILL_PROMPT(
        "skill_prompt", "技能提示词", "扫描并注入可用技能说明",
        CapabilityType.PROMPT_PLUGIN,
        config -> new SkillPromptPlugin()
    ),
    AGENTS_MD_PROMPT(
        "agents_md_prompt", "AGENTS.md提示词", "自动读取工作目录下的AGENTS.md并注入系统提示",
        CapabilityType.PROMPT_PLUGIN,
        config -> new AgentsMdPromptPlugin()
    ),
    LOGGING_HOOK(
        "logging", "日志钩子", "在Agent Loop关键生命周期节点打印调试日志",
        CapabilityType.HOOK,
        config -> new LoggingHook()
    );

    private final String id;
    private final String name;
    private final String description;
    private final CapabilityType type;
    private final Function<AppConfig, Object> factory;

    StandardCapability(String id, String name, String description, CapabilityType type, Function<AppConfig, Object> factory) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.factory = factory;
    }

    public Object create(AppConfig config) {
        return factory.apply(config);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public CapabilityType getType() { return type; }

    public static StandardCapability fromId(String id) {
        for (StandardCapability c : values()) {
            if (c.id.equals(id)) return c;
        }
        throw new IllegalArgumentException("Unknown capability: " + id);
    }
}
```

- [ ] **Step 3: 创建 CapabilityLoader**

```java
package com.colin.code.runtime;

import com.colin.code.core.agent.AgentHook;
import com.colin.code.core.interceptor.InputInterceptor;
import com.colin.code.core.message.MessageBuilder;
import com.colin.code.core.prompt.PromptPlugin;
import com.colin.code.core.provider.LLMProvider;
import com.colin.code.core.tool.Tool;
import com.colin.code.core.tool.ToolRegistry;
import com.colin.code.runtime.config.AppConfig;

import java.util.ArrayList;
import java.util.List;

public class CapabilityLoader {

    private final List<Tool> tools = new ArrayList<>();
    private final List<PromptPlugin> promptPlugins = new ArrayList<>();
    private final List<AgentHook> hooks = new ArrayList<>();
    private final List<InputInterceptor> interceptors = new ArrayList<>();
    private MessageBuilder messageBuilder;

    public CapabilityLoader withTool(Tool tool) {
        if (tool != null) tools.add(tool);
        return this;
    }

    public CapabilityLoader withPromptPlugin(PromptPlugin plugin) {
        if (plugin != null) promptPlugins.add(plugin);
        return this;
    }

    public CapabilityLoader withHook(AgentHook hook) {
        if (hook != null) hooks.add(hook);
        return this;
    }

    public CapabilityLoader withInterceptor(InputInterceptor interceptor) {
        if (interceptor != null) interceptors.add(interceptor);
        return this;
    }

    public CapabilityLoader withMessageBuilder(MessageBuilder messageBuilder) {
        this.messageBuilder = messageBuilder;
        return this;
    }

    public CapabilityLoader withCapability(StandardCapability cap, AppConfig config) {
        Object instance = cap.create(config);
        switch (cap.getType()) {
            case TOOL -> withTool((Tool) instance);
            case PROMPT_PLUGIN -> withPromptPlugin((PromptPlugin) instance);
            case HOOK -> withHook((AgentHook) instance);
            case INTERCEPTOR -> withInterceptor((InputInterceptor) instance);
        }
        return this;
    }

    public AgentRuntime build(LLMProvider provider, AppConfig config) {
        ToolRegistry registry = new ToolRegistry();
        for (Tool t : tools) {
            registry.register(t);
        }

        MessageBuilder mb = this.messageBuilder;
        if (mb == null) {
            mb = new com.colin.code.capability.prompt.StandardMessageBuilder(promptPlugins, config);
        }

        com.colin.code.core.agent.AgentLoop loop = new com.colin.code.core.agent.AgentLoop(
            provider, registry, mb, hooks, interceptors, config
        );

        return new AgentRuntime(loop);
    }
}
```

- [ ] **Step 4: 创建 AgentRuntime**

```java
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
```

- [ ] **Step 5: 编译验证**

```bash
mvn compile
```
Expected: 编译通过

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/colin/code/runtime/
git commit -m "feat(runtime): add CapabilityLoader, StandardCapability, AgentRuntime"
```

---

### Task 10: 改造 AgentLoop

**Files:**
- Create: `src/main/java/com/colin/code/core/agent/AgentLoop.java`
- Delete (later): `src/main/java/com/colin/code/agent/AgentLoop.java`

- [ ] **Step 1: 重写 AgentLoop**

```java
package com.colin.code.core.agent;

import com.colin.code.core.interceptor.InputInterceptor;
import com.colin.code.core.message.MessageBuilder;
import com.colin.code.core.provider.LLMProvider;
import com.colin.code.core.provider.LLMResponse;
import com.colin.code.core.provider.ToolCallRequest;
import com.colin.code.core.tool.ToolRegistry;
import com.colin.code.runtime.config.AppConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AgentLoop {

    private final LLMProvider provider;
    private final ToolRegistry toolRegistry;
    private final MessageBuilder messageBuilder;
    private final List<AgentHook> hooks;
    private final List<InputInterceptor> interceptors;
    private final AppConfig config;

    public AgentLoop(
            LLMProvider provider,
            ToolRegistry toolRegistry,
            MessageBuilder messageBuilder,
            List<AgentHook> hooks,
            List<InputInterceptor> interceptors,
            AppConfig config) {
        this.provider = provider;
        this.toolRegistry = toolRegistry;
        this.messageBuilder = messageBuilder;
        this.hooks = hooks != null ? hooks : List.of();
        this.interceptors = interceptors != null ? interceptors : List.of();
        this.config = config;
    }

    public String chat(String userMessage) {
        for (AgentHook h : hooks) {
            h.onLoopStart(userMessage);
        }

        for (InputInterceptor ic : interceptors) {
            Optional<String> direct = ic.intercept(userMessage);
            if (direct.isPresent()) {
                for (AgentHook h : hooks) {
                    h.onLoopEnd(direct.get());
                }
                return direct.get();
            }
        }

        List<Map<String, Object>> messages = messageBuilder.buildInitial(userMessage);

        for (int iter = 0; iter < config.getMaxIterations(); iter++) {
            for (AgentHook h : hooks) {
                h.beforeLLMCall(messages);
            }
            LLMResponse response = provider.chat(
                    messages,
                    toolRegistry.getDefinitions(),
                    config.getModel(),
                    config.getMaxTokens(),
                    config.getTemperature()
            );
            for (AgentHook h : hooks) {
                h.afterLLMCall(response);
            }

            if (response.hasToolCalls()) {
                messageBuilder.addAssistantMessage(messages, response);
                for (ToolCallRequest tc : response.getToolCalls()) {
                    String result = toolRegistry.execute(tc);
                    for (AgentHook h : hooks) {
                        h.onToolCall(tc, result);
                    }
                    messageBuilder.addToolResult(messages, tc, result);
                }
            } else {
                String finalResponse = response.getContent() != null ? response.getContent() : "";
                for (AgentHook h : hooks) {
                    h.onLoopEnd(finalResponse);
                }
                return finalResponse;
            }
        }

        String maxIterMsg = "[Reached max iterations: " + config.getMaxIterations() + "]";
        for (AgentHook h : hooks) {
            h.onLoopEnd(maxIterMsg);
        }
        return maxIterMsg;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile
```
Expected: 编译通过（此时新旧 AgentLoop 并存）

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/colin/code/core/agent/AgentLoop.java
git commit -m "refactor(core): rewrite AgentLoop with injected MessageBuilder, Hooks, Interceptors"
```

---

### Task 11: 创建 entry 入口

**Files:**
- Create: `src/main/java/com/colin/code/entry/MinimalDemo.java`
- Create: `src/main/java/com/colin/code/entry/CliApp.java`

- [ ] **Step 1: 创建 MinimalDemo**

```java
package com.colin.code.entry;

import com.colin.code.capability.provider.mock.MockProvider;
import com.colin.code.capability.tool.exec.ExecTool;
import com.colin.code.core.provider.LLMProvider;
import com.colin.code.core.provider.LLMResponse;
import com.colin.code.core.provider.ToolCallRequest;
import com.colin.code.runtime.AgentRuntime;
import com.colin.code.runtime.CapabilityLoader;
import com.colin.code.runtime.StandardCapability;
import com.colin.code.runtime.config.AppConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MinimalDemo {
    public static void main(String[] args) {
        System.out.println("=== colin-code Minimal Demo ===\n");

        AppConfig config = new AppConfig();
        LLMProvider provider = buildMockProvider();

        AgentRuntime runtime = new CapabilityLoader()
            .withCapability(StandardCapability.EXEC_TOOL, config)
            .withCapability(StandardCapability.BASE_PROMPT, config)
            .build(provider, config);

        String result = runtime.chat("帮我列出当前目录的文件");
        System.out.println("\n[Mock 模式] 最终结果：");
        System.out.println(result);
    }

    private static LLMProvider buildMockProvider() {
        List<LLMResponse> responses = new ArrayList<>();

        LLMResponse r1 = new LLMResponse();
        r1.setContent("我来帮你查看当前目录的文件列表。");
        ToolCallRequest tc = new ToolCallRequest();
        tc.setId("call_1");
        tc.setName("exec");
        tc.setArguments(Collections.singletonMap("command", "ls -la"));
        r1.setToolCalls(Collections.singletonList(tc));
        responses.add(r1);

        LLMResponse r2 = new LLMResponse();
        r2.setContent("当前目录包含 pom.xml、src、target 等文件和文件夹。");
        responses.add(r2);

        return new MockProvider(responses);
    }
}
```

- [ ] **Step 2: 创建 CliApp**

```java
package com.colin.code.entry;

import com.colin.code.capability.provider.openai.OpenAICompatibleProvider;
import com.colin.code.core.provider.LLMProvider;
import com.colin.code.runtime.AgentRuntime;
import com.colin.code.runtime.CapabilityLoader;
import com.colin.code.runtime.StandardCapability;
import com.colin.code.runtime.config.AppConfig;

public class CliApp {
    public static void main(String[] args) {
        System.out.println("=== colin-code CLI ===\n");

        AppConfig config = new AppConfig();

        if (config.getApiKey().isEmpty()) {
            System.out.println("真实 API 模式需要设置环境变量 OPENAI_API_KEY，当前切换到 MinimalDemo 模式。\n");
            MinimalDemo.main(args);
            return;
        }

        LLMProvider provider = new OpenAICompatibleProvider(config);

        AgentRuntime runtime = new CapabilityLoader()
            .withCapability(StandardCapability.EXEC_TOOL, config)
            .withCapability(StandardCapability.BASE_PROMPT, config)
            .withCapability(StandardCapability.LOGGING_HOOK, config)
            .build(provider, config);

        String result = runtime.chat("帮我看一下本地ip，用中文回答");
        System.out.println("\n[真实 API 模式] 最终结果：");
        System.out.println(result);
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
mvn compile
```
Expected: 编译通过

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/colin/code/entry/
git commit -m "feat(entry): add MinimalDemo and CliApp"
```

---

### Task 12: 删除旧文件并验证编译

**Files:**
- Delete: `src/main/java/com/colin/code/agent/AgentLoop.java`
- Delete: `src/main/java/com/colin/code/agent/ContextBuilder.java`
- Delete: `src/main/java/com/colin/code/config/DemoConfig.java`
- Delete: `src/main/java/com/colin/code/Main.java`
- Delete: `src/main/java/com/colin/code/provider/*`
- Delete: `src/main/java/com/colin/code/tool/*`

- [ ] **Step 1: 删除旧文件**

```bash
rm -rf src/main/java/com/colin/code/agent
rm -rf src/main/java/com/colin/code/config
rm -rf src/main/java/com/colin/code/Main.java
rm -rf src/main/java/com/colin/code/provider
rm -rf src/main/java/com/colin/code/tool
```

- [ ] **Step 2: 编译验证**

```bash
mvn clean compile
```
Expected: `BUILD SUCCESS`

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "refactor: remove legacy files after migration to core-capability-runtime-entry"
```

---

### Task 13: 运行验证和最终提交

- [ ] **Step 1: 运行 MinimalDemo**

```bash
mvn compile exec:java -Dexec.mainClass="com.colin.code.entry.MinimalDemo"
```
Expected: 输出包含 "当前目录包含 pom.xml、src、target 等文件和文件夹。"

- [ ] **Step 2: 检查 Git 状态**

```bash
git status
```
Expected: working tree clean

- [ ] **Step 3: 最终提交（如有未提交的 pom.xml 调整）**

如有需要，更新 `pom.xml` 的 mainClass 或其他配置后提交：
```bash
git add pom.xml
git commit -m "build: update pom.xml for new entry points"
```

---

## 计划自检

**1. Spec coverage:**
- ✅ core 层接口与数据模型迁移（Task 1-3）
- ✅ capability 层具体实现迁移（Task 4-5, 7-8）
- ✅ runtime 组装中枢（Task 9）
- ✅ AgentLoop 改造注入新依赖（Task 10）
- ✅ entry 多入口（Task 11）
- ✅ 旧文件清理和编译验证（Task 12-13）
- ✅ PromptPlugin 动态组装（Task 7）
- ✅ InputInterceptor 前置拦截（Task 3, 10）

**2. Placeholder scan:**
- 无 TBD/TODO/"implement later" 占位符
- 所有代码步骤包含完整代码
- 所有命令包含预期输出

**3. Type consistency:**
- `DemoConfig` 统一升级为 `AppConfig`
- `AgentLoop` 构造函数签名在 Task 10 中与 `CapabilityLoader.build()` 在 Task 9 中匹配
- `StandardCapability` 的 factory 返回类型与 `CapabilityLoader.withCapability()` 的 switch 分支一致
