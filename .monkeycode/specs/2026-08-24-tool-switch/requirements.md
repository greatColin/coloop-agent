# 需求文档：Web端可配置的工具开关

## Introduction

本需求在 Web 设置界面新增「工具」Tab，允许用户单独开启/关闭每个内置工具（Exec、ReadFile、WriteFile、EditFile、SearchFiles、ListDirectory、MCP Client、Task Management 等），开关状态持久化到 SQLite 数据库，默认全部开启。

## Glossary

- **Tool Switch（工具开关）**：控制单个工具是否注册到 Agent Runtime 的布尔配置项。
- **ToolRegistry**：core 模块的工具注册表，负责管理所有可用工具并向 LLM 输出 function 定义列表。

## Requirements

### Requirement 1: 工具开关配置持久化

**User Story:** AS 用户, I want 在 Web 设置中开关工具并持久化保存, so that 不需要的工具不会出现在 LLM 的工具列表中。

#### Acceptance Criteria

1. WHEN 服务首次启动，WHILE SQLite 配置库中不存在工具开关数据，系统 SHALL 将所有工具默认设为启用状态（`enabled: true`）写入配置库。
2. WHEN 用户在 Web 设置中修改工具开关并点击保存，系统 SHALL 将更新后的工具开关配置写入 SQLite 配置库。
3. WHEN 服务启动或新会话构建时，系统 SHALL 从 SQLite 加载工具开关配置，未启用的工具 SHALL 不注册到 ToolRegistry。
4. WHEN 用户未修改任何开关直接关闭设置面板，系统 SHALL 不执行任何保存操作。

### Requirement 2: Web 设置界面工具 Tab

**User Story:** AS 用户, I want 在设置界面看到所有工具的开关列表, so that 我可以方便地开启或关闭特定工具。

#### Acceptance Criteria

1. WHEN 用户点击「设置」按钮 THEN 系统 SHALL 在设置面板中新增「工具」Tab。
2. WHEN 用户切换到「工具」Tab THEN 系统 SHALL 显示所有工具的开关列表，每项包含工具名称、描述和开关 Toggle。
3. WHEN 用户点击开关 TOGGLE THEN 系统 SHALL 更新该工具的启用状态（视觉反馈即时更新）。
4. WHEN 用户点击「保存」THEN 系统 SHALL 校验并保存所有开关配置到 SQLite，返回成功提示。

### Requirement 3: 工具开关影响 LLM 函数调用

**User Story:** AS 用户, I want 关闭的工具不会出现在 LLM 可调用的函数列表中, so that LLM 无法调用已禁用的工具。

#### Acceptance Criteria

1. WHEN AgentLoop 构建时，系统 SHALL 仅将 `enabled: true` 的工具注册到 ToolRegistry。
2. WHEN ToolRegistry.getDefinitions() 被调用 THEN 系统 SHALL 仅返回已启用工具的 function 定义。
3. WHEN 用户调用已禁用的工具名称 THEN 系统 SHALL 返回 `[Error: tool not found: xxx]`。

## Non-Functional Requirements

1. 工具开关配置与模型/MCP 等配置共存于同一 SQLite 表或独立表均可（实现方决定）。
2. 新增工具时（StandardCapability 新增条目）默认启用，无需额外操作。
3. 关闭核心工具（如 exec）不应导致系统崩溃，仅该工具不可用。

## Out of Scope

1. MCP Server 级别的启用/禁用（MCP Client 工具本身可关闭，但 MCP Server 配置管理不在本需求范围内）。
2. 动态修改开关后立即生效（下一会话生效，与配置加载机制一致）。
