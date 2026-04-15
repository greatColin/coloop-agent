# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

`colin-code` 是一个精简版 Java Agent 学习演示项目，展示了最小可用的 agent-loop + exec 工具流程。项目基于 Maven 构建，使用 JDK 21，主要依赖 Jackson 和 OkHttp。

## 常用命令

- **编译并运行 Demo**：
  ```bash
  mvn compile exec:java -Dexec.mainClass="com.colin.code.Main"
  ```
- **编译**：`mvn compile`
- **打包**：`mvn package`
- **清理**：`mvn clean`

## 架构设计

### 核心流程

程序入口为 `com.colin.code.Main`，支持两种运行模式：

1. **Mock 模式（默认）**：不依赖外部 API，使用 `MockProvider` 模拟两轮 LLM 响应，验证 agent-loop 逻辑。
2. **真实 API 模式**：需设置环境变量 `OPENAI_API_KEY`，使用 `OpenAICompatibleProvider` 调用真实模型。

### 模块职责

- **`agent.AgentLoop`**：Agent 核心循环。负责构造消息列表，在 `while` 循环中调用 LLM，如返回 `tool_calls` 则执行工具并追加结果到上下文，否则返回最终文本回复。
- **`agent.ContextBuilder`**：上下文构建器。组装 System Prompt 和消息列表。System Prompt 由 `PromptSegment` 枚举拼接而成，并注入当前时间、工作目录、平台等动态信息。
- **`prompt.PromptSegment`**：系统提示词段落枚举。标记为 `enabled` 的段落会注入到 System Prompt 中；`disabled` 的段落仅作为知识库保留。
- **`provider.LLMProvider`**：LLM 提供商接口，定义对话补全方法。
  - `OpenAICompatibleProvider`：调用 OpenAI 兼容 API（Chat Completions）。
  - `MockProvider`：按预设列表顺序返回模拟响应，用于测试 loop 逻辑。
- **`tool.ToolRegistry`**：工具注册表。管理所有可用工具，向 LLM 输出 function 定义列表，并调度工具执行。
- **`tool.ExecTool`**：当前唯一实现的工具，用于执行 shell 命令，支持超时控制（默认 30 秒），自动适配 Windows / Unix 环境。
- **`config.DemoConfig`**：配置类，包含模型参数、API 连接信息、最大迭代次数、执行超时等默认值。API Key 可通过环境变量 `OPENAI_API_KEY` 注入。

### 工具扩展方式

如需添加新工具：
1. 实现 `tool.Tool` 接口（或继承 `tool.BaseTool`）。
2. 在 `AgentLoop.registerTools()` 中通过 `toolRegistry.register()` 注册。

## 运行真实 API 的注意事项

切换至真实 API 模式时，需确保：
- 环境变量 `OPENAI_API_KEY` 已设置。
- `DemoConfig` 中的 `apiBase` 和 `model` 已配置为正确的端点和模型名。
- 在 `Main.java` 中注释掉 `runWithMock()`，取消注释 `runWithRealAPI()`。
