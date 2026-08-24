# 需求文档：模型配置持久化到 SQLite 并支持 Web 设置界面

## Introduction

当前 coloop-agent 的模型、MCP、语音等配置全部存放在 `coloop-agent-core/src/main/resources/coloop-agent-setting.json` 配置文件中，用户修改配置需要直接编辑文件并重启服务，体验差且易出错。

本特性将配置持久化迁移到本地 SQLite 数据库，并在 Web UI 左下角新增「设置」按钮，点击后弹出配置表单，用户可在线编辑模型、MCP 等配置。现有配置文件内容作为首次启动的引导默认值（seed）与参考备注保留。

同时，为保证提交/推送的提交记录与远程仓库作者一致，需要建立本地 Git 作者信息与远程作者同步的校验机制。

## Glossary

- **配置项**：指 `defaultModel`、`maxIterations`、`execTimeoutSeconds`、`models`、`mcpServers`、`voice` 等应用配置字段。
- **模型配置（ModelConfig）**：单个模型条目的字段集合，包括 key、description、model、apiKey、apiBase、maxTokens、temperature、maxContextSize。
- **MCP 服务器配置（McpServerConfig）**：单个 MCP 服务器的字段集合，包括 key、command、args、env。
- **引导默认值（seed）**：`coloop-agent-setting.json` 文件中的配置内容，仅在 SQLite 配置库为空时导入，作为初始配置与字段备注参考。
- **远程作者（remote author）**：本项目目标提交作者，即 author name = `greatColin`，author email = `34583190+greatColin@users.noreply.github.com`（GitHub noreply 邮箱）。

## Requirements

### Requirement 1: 配置持久化到 SQLite

**User Story:** AS 用户, I want 将模型/MCP 等配置保存在 SQLite 数据库而非配置文件, so that 无需修改文件即可完成配置管理。

#### Acceptance Criteria

1. WHEN 服务首次启动，WHILE SQLite 配置库中不存在配置数据，系统 SHALL 将 `coloop-agent-setting.json` 解析后的配置作为引导默认值写入配置库。
2. WHEN 服务启动时，系统 SHALL 从 SQLite 配置库加载配置以组装 Agent Runtime；SQLite 有数据时 SHALL 优先于配置文件。
3. WHEN 配置库中无任何配置数据，系统 SHALL 回退加载 `coloop-agent-setting.json` 作为运行配置。
4. WHEN 配置被保存后，系统 SHALL 将更新持久化到 SQLite 配置库，供后续重启与后续会话使用。

### Requirement 2: Web 设置入口与表单

**User Story:** AS 用户, I want 在页面左下角点击设置按钮弹出表单编辑模型与 MCP 配置, so that 在页面上即可完成配置的查看与修改。

#### Acceptance Criteria

1. WHEN 页面加载完成，系统 SHALL 在侧边栏底部展示「设置」入口按钮。
2. WHEN 用户点击「设置」按钮，系统 SHALL 弹出配置表单模态框，并展示当前数据库中的配置内容。
3. WHEN 表单中新增或移除模型条目，系统 SHALL 支持动态增删模型，字段包括 key、description、model、apiKey、apiBase、maxTokens、temperature、maxContextSize。
4. WHEN 表单中新增或移除 MCP 服务器条目，系统 SHALL 支持动态增删 MCP，字段包括 key、command、args、env。
5. WHEN 表单中修改 `defaultModel`、`maxIterations`、`execTimeoutSeconds`，系统 SHALL 支持对应全局配置的编辑。
6. WHEN 用户点击「保存」，系统 SHALL 校验必填字段与格式，并将配置写入 SQLite 配置库。
7. WHEN 配置保存成功，系统 SHALL 提示用户新配置将在下一个会话生效，且后续新建会话 SHALL 使用新配置。
8. WHEN 配置保存失败（如字段缺失、JSON 格式错误），系统 SHALL 展示明确错误提示且不覆盖原配置。

### Requirement 3: 配置引导值与备注展示

**User Story:** AS 用户, I want 看到配置文件中原始内容的参考, so that 了解默认值与字段含义。

#### Acceptance Criteria

1. WHEN 设置表单加载，系统 SHALL 在表单中展示配置文件中的原始内容作为参考备注。
2. WHEN 用户修改配置，系统 SHALL 保留配置文件原始内容不被覆盖，仅作为引导参考。

### Requirement 4: 模型与 MCP 配置形式参考其他 Agent

**User Story:** AS 用户, I want 模型与 MCP 的配置形式与主流 Agent 工具保持一致, so that 降低配置迁移成本。

#### Acceptance Criteria

1. WHEN 设计模型配置表单，系统 SHALL 采用与主流 Agent 一致的字段形式（name、description、apiKey、apiBase、model、maxContextSize 等）。
2. WHEN 设计 MCP 配置表单，系统 SHALL 支持 command + args + env 形式，兼容 STDIO 类 MCP 服务器。

### Requirement 5: Git 提交作者与远程仓库同步

**User Story:** AS 开发者, I want 提交记录作者统一为 greatColin，并在提交/推送前校验本地作者信息, so that 提交记录作者身份与远程仓库保持一致。

#### Acceptance Criteria

1. WHEN 执行 `git commit`，系统 SHALL 校验本地 `user.name` 为 `greatColin` 且 `user.email` 为 `34583190+greatColin@users.noreply.github.com`。
2. WHEN 执行 `git push`，系统 SHALL 校验本地作者信息与上述目标一致。
3. WHEN 本地作者信息与目标不一致，系统 SHALL 中断提交或推送操作，并提示正确的 author name 与 author email。
4. WHEN 本地作者信息与目标一致，系统 SHALL 正常执行提交与推送。
5. WHEN 重写历史提交作者信息，系统 SHALL 将历史提交的 author 与 committer 统一重写为 `greatColin <34583190+greatColin@users.noreply.github.com>`。
6. WHEN 历史提交重写完成，系统 SHALL 备份原始仓库数据，并经用户确认后使用 force push 覆盖远程仓库。
