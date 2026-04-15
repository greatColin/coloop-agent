# colin-code 架构重构设计文档

## 1. 设计目标

将 `colin-code` 从一个最小可用的 Agent-loop 演示项目，演进为一个**可插拔、模块隔离、教学友好的轻量级 AGI Agent 底座**。

核心约束：
- `core` 始终精简，教学时能一眼看到最基础循环
- 新增/删除能力不改动现有代码
- 配置驱动与链式组装共用同一套逻辑
- 为语音、虚拟形象、任务规划、多 Agent 等远期能力预留扩展点

---

## 2. 整体架构：核心-插件洋葱模型

```
com.colin.code
├── core/                 ← 教学核心，永远最小化
│   ├── agent/            ← AgentLoop + AgentHook
│   ├── message/          ← MessageBuilder 接口
│   ├── provider/         ← LLMProvider / LLMResponse / ToolCallRequest
│   └── tool/             ← Tool / BaseTool / ToolRegistry
├── capability/           ← 可插拔能力模块，独立包，互不依赖
│   ├── provider/openai/
│   ├── provider/mock/
│   ├── tool/exec/
│   ├── tool/filesystem/
│   ├── prompt/
│   ├── hook/
│   └── ...               ← 未来：planning/ voice/ avatar/
├── runtime/              ← 动态组装中枢
│   ├── CapabilityLoader.java
│   ├── StandardCapability.java   ← 内置能力目录枚举
│   ├── CapabilityType.java
│   ├── AgentRuntime.java
│   └── config/AppConfig.java
└── entry/                ← 多入口
    ├── MinimalDemo.java  ← 教学专用，极简
    └── CliApp.java       ← 完整 CLI
```

---

## 3. 模块边界与接口契约

### 3.1 core 包

`core` 只保留抽象接口和骨架实现，**不依赖任何具体 HTTP 库、不实现任何具体工具**。

| 组件 | 职责 |
|------|------|
| `AgentLoop` | `while` 循环：调用 LLM → 判断 tool_calls → 执行工具 → 追加结果 |
| `AgentHook` | 生命周期钩子接口（onLoopStart / beforeLLMCall / afterLLMCall / onToolCall / onLoopEnd） |
| `MessageBuilder` | 抽象消息构造器：buildInitial / addAssistantMessage / addToolResult |
| `InputInterceptor` | 输入拦截器：在 LLM 调用前检查用户输入，可直接短路返回 |
| `LLMProvider` | LLM 调用抽象 |
| `Tool` / `ToolRegistry` | 工具接口和注册表 |

`AgentLoop` 的依赖通过构造函数注入：
```java
public AgentLoop(
    LLMProvider provider,
    ToolRegistry toolRegistry,
    MessageBuilder messageBuilder,
    List<AgentHook> hooks,
    AppConfig config
)
```

### 3.2 capability 包

每个子包是一个独立能力模块，**模块之间不允许直接依赖**，只能通过 `core` 的公共接口交互。

| 模块 | 说明 |
|------|------|
| `provider/openai` | `OpenAICompatibleProvider`，依赖 OkHttp + Jackson |
| `provider/mock` | `MockProvider`，用于测试 loop 逻辑 |
| `tool/exec` | `ExecTool`，Shell 命令执行 |
| `tool/filesystem` | `FileReadTool`、`FileWriteTool`、`GlobTool`、`GrepTool` |
| `prompt` | 多个 `PromptPlugin` 实现 |
| `hook` | `LoggingHook` 等生命周期实现 |

### 3.3 runtime 包

`CapabilityLoader` 是唯一的组装中枢，提供统一的链式 API。

```java
AgentRuntime runtime = new CapabilityLoader()
    .withCapability(StandardCapability.EXEC_TOOL, config)
    .withCapability(StandardCapability.BASE_PROMPT, config)
    .withTool(new MyCustomTool())
    .build(provider, config);
```

---

## 4. 动态 Prompt 组装

### 4.1 PromptPlugin 接口

```java
public interface PromptPlugin {
    String getName();
    int getPriority();                       // 数字越小越靠前
    String generate(AppConfig config, Map<String, Object> runtimeContext);
}
```

### 4.2 CompositeMessageBuilder

`runtime` 层实现 `MessageBuilder`，按优先级收集所有 `PromptPlugin` 的输出，拼接成最终的 system message。

### 4.3 内置 PromptPlugin

| 插件 | 优先级 | 职责 |
|------|--------|------|
| `BasePromptPlugin` | 0 | 基础身份、环境信息、输出规范 |
| `SkillPromptPlugin` | 10 | 扫描并注入可用技能说明 |
| `AgentsMdPromptPlugin` | 20 | 自动读取 `AGENTS.md` 注入系统提示 |

> 新增 prompt 来源只需新增 `PromptPlugin` 实现，无需改动 `core` 或 `runtime` 现有代码。

---

## 5. StandardCapability 内置能力目录

统一枚举所有内置能力，作为**规范目录**和**简化配置**的入口。

```java
public enum StandardCapability {
    EXEC_TOOL("exec", "Shell执行工具", CapabilityType.TOOL, ...),
    FILE_READ_TOOL("file_read", "文件读取工具", CapabilityType.TOOL, ...),
    BASE_PROMPT("base_prompt", "基础提示词", CapabilityType.PROMPT_PLUGIN, ...),
    SKILL_PROMPT("skill_prompt", "技能提示词", CapabilityType.PROMPT_PLUGIN, ...),
    AGENTS_MD_PROMPT("agents_md_prompt", "AGENTS.md提示词", CapabilityType.PROMPT_PLUGIN, ...),
    LOGGING_HOOK("logging", "日志钩子", CapabilityType.HOOK, ...);
}
```

### 使用方式

**教学 demo（纯链式）：**
```java
new CapabilityLoader()
    .withCapability(StandardCapability.EXEC_TOOL, config)
    .withCapability(StandardCapability.BASE_PROMPT, config)
    .build(provider, config);
```

**配置驱动（引用枚举 ID）：**
```json
{
  "capabilities": [
    "exec",
    "file_read",
    "base_prompt",
    "skill_prompt",
    "agents_md_prompt",
    "logging"
  ]
}
```
```java
new CapabilityLoader()
    .fromConfig("classpath:default-capabilities.json", config)
    .build(provider, config);
```

> 配置文件中 `capabilities` 数组用短 ID 引用枚举，极大简化。自定义能力仍可通过 `tools`/`promptPlugins`/`hooks` 数组用完整类名补充。

---

## 6. Hook 机制设计

`AgentHook` 作为 `CapabilityLoader` 的一级公民，与 Tool、PromptPlugin 平级参与链式组装。

```java
public interface AgentHook {
    default void onLoopStart(String userMessage) {}
    default void beforeLLMCall(List<Map<String, Object>> messages) {}
    default void afterLLMCall(LLMResponse response) {}
    default void onToolCall(ToolCallRequest toolCall, String result) {}
    default void onLoopEnd(String finalResponse) {}
}
```

一个 capability 可以同时是 Tool 和 Hook：
```java
public class LoggingCapability implements Tool, AgentHook {
    // 注册时：loader.withTool(logCap).withHook(logCap)
}
```

---

## 7. InputInterceptor 前置拦截器

新增 `core.interceptor.InputInterceptor` 接口，用于在 LLM 调用之前检查用户输入，实现**快捷指令、Skill 系统、手动上下文压缩**等直接返回功能。

### 接口定义

```java
package com.colin.code.core.interceptor;

import java.util.Optional;

public interface InputInterceptor {
    Optional<String> intercept(String userMessage);
}
```

### AgentLoop 中的接入点

```java
public String chat(String userMessage) {
    for (InputInterceptor ic : interceptors) {
        Optional<String> direct = ic.intercept(userMessage);
        if (direct.isPresent()) {
            hooks.forEach(h -> h.onLoopEnd(direct.get()));
            return direct.get();
        }
    }
    // ... 原有 LLM 循环逻辑
}
```

### CapabilityLoader 链式 API

```java
new CapabilityLoader()
    .withInterceptor(new CompactInterceptor(history))
    .withInterceptor(new SkillDispatcher(skills))
    .build(provider, config);
```

### 典型应用场景

| 场景 | 实现方式 |
|------|----------|
| `/compact` 手动压缩 | `CompactInterceptor` 检测输入，直接调用压缩逻辑并返回 |
| `/commit` Skill | `SkillDispatcher` 解析 `/xxx`，分发到对应 Skill 执行 |
| 权限确认拦截 | `PermissionInterceptor` 对敏感命令弹窗确认或直接拒绝 |

> `InputInterceptor` 与 `PromptPlugin`、`Tool`、`AgentHook` 完全正交，各司其职。

---

## 8. 当前代码迁移路径

### 7.1 文件移动清单

| 原路径 | 新路径 |
|--------|--------|
| `agent/AgentLoop.java` | `core/agent/AgentLoop.java` |
| `agent/ContextBuilder.java` | `capability/prompt/StandardMessageBuilder.java` |
| `provider/LLMProvider.java` | `core/provider/LLMProvider.java` |
| `provider/LLMResponse.java` | `core/provider/LLMResponse.java` |
| `provider/ToolCallRequest.java` | `core/provider/ToolCallRequest.java` |
| `provider/MockProvider.java` | `capability/provider/mock/MockProvider.java` |
| `provider/OpenAICompatibleProvider.java` | `capability/provider/openai/OpenAICompatibleProvider.java` |
| `tool/Tool.java` | `core/tool/Tool.java` |
| `tool/BaseTool.java` | `core/tool/BaseTool.java` |
| `tool/ToolRegistry.java` | `core/tool/ToolRegistry.java` |
| `tool/ExecTool.java` | `capability/tool/exec/ExecTool.java` |
| `config/DemoConfig.java` | `runtime/config/AppConfig.java` |
| `Main.java` | `entry/CliApp.java` + `entry/MinimalDemo.java` |
| `prompt/PromptSegment.java` | `capability/prompt/PromptSegment.java` |

### 7.2 新增文件

| 新文件 | 职责 |
|--------|------|
| `core/message/MessageBuilder.java` | 消息构建抽象 |
| `core/agent/AgentHook.java` | 生命周期钩子 |
| `core/interceptor/InputInterceptor.java` | 输入拦截器 |
| `capability/prompt/BasePromptPlugin.java` | 基础提示词 |
| `capability/prompt/SkillPromptPlugin.java` | 技能注入 |
| `capability/prompt/AgentsMdPromptPlugin.java` | AGENTS.md 注入 |
| `capability/hook/LoggingHook.java` | 示例钩子 |
| `runtime/CapabilityLoader.java` | 组装中枢 |
| `runtime/StandardCapability.java` | 能力目录枚举 |
| `runtime/CapabilityType.java` | 能力类型枚举 |
| `runtime/AgentRuntime.java` | 可运行代理 |

### 7.3 迁移步骤

1. 新建目录结构 `core/` / `capability/` / `runtime/` / `entry/`
2. 移动接口与数据模型到 `core`
3. 移动具体实现到 `capability` 对应子包
4. 改造 `ContextBuilder` → `StandardMessageBuilder`，实现 `MessageBuilder` ****接口****
5. 拆分 `PromptSegment` 拼接逻辑为多个 `PromptPlugin`
6. 新增 `CapabilityLoader`、`StandardCapability`、`AgentHook`、`InputInterceptor`
7. 改造 `AgentLoop`，注入 `MessageBuilder`、`AgentHook` 列表和 `InputInterceptor` 列表
8. 升级 `DemoConfig` → `AppConfig`
9. 拆分 `Main.java` 为 `MinimalDemo` 和 `CliApp`
10. 更新 `pom.xml`、编译验证、更新 `CLAUDE.md`

---

## 8. AGI 远期扩展点预留

### 8.1 语音输入/输出

增加 `io.UserInterface` 抽象层：
```java
public interface UserInterface {
    String readInput();
    void writeOutput(String text);
}
```

- `entry.CliApp` 使用 `ConsoleUI`
- 未来 `entry.VoiceApp` 替换为 `SpeechUI`
- `core.AgentLoop` 完全无需改动

### 8.2 虚拟形象

作为 capability 存在，通过 `AgentHook` 驱动：
```java
public class AvatarHook implements AgentHook {
    public void beforeLLMCall(...) { driver.playThinking(); }
    public void onLoopEnd(String response) { driver.speak(response); }
}
```

### 8.3 任务规划

```java
public interface Planner {
    List<SubTask> decompose(String userRequest);
}
```

接入方式：
- 作为 Tool：`plan` 工具返回子任务列表
- 作为 Pre-Hook：在 `onLoopStart` 中预先生成规划并注入 messages

### 8.4 多 Agent

**方式A：Agent 作为 Tool**
将 `AgentRuntime` 包装成 `AgentTool` 注册到 `ToolRegistry`。

**方式B：GroupChatRuntime**
新增编排器管理多个 `AgentRuntime`，`core.AgentLoop` 本身不改。

---

## 9. 决策记录（ADR）

| 决策 | 原因 |
|------|------|
| 去掉 SPI 机制 | 降低复杂度，统一为链式逻辑 |
| 配置驱动也走链式 | `fromConfig()` 本质是批量调用 `withXxx()`，无两套逻辑 |
| Prompt 按插件优先级拼接 | 支持 Skill、AGENTS.md 等动态注入，无需改核心 |
| 枚举目录 `StandardCapability` | 规范内置能力 ID，简化配置，同时作为能力目录 |
| Hook 与 Tool 平级 | 统一组装心智模型，支持一个类多重身份 |
| core 保持无反射 | 唯一反射隔离在 `CapabilityLoader.fromConfig()` 中 |
| 新增 `InputInterceptor` | 支持 Skill、快捷指令等直接短路返回，与 Tool/PromptPlugin/Hook 正交 |

---

*设计日期：2026-04-15*
*目标分支：main*
