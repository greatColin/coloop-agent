# AGENTS.md - 项目约定

## 项目简介
coloop-agent 是一个基于 Java 的 AI Agent 框架，支持工具调用和多轮对话。

## 编码规范
- 使用 Java 17+ 特性
- 遵循阿里巴巴 Java 编码规范
- 类名使用 PascalCase，方法名使用 camelCase

## 项目结构
- `src/main/java/com/coloop/agent/entry/` - 入口类
- `src/main/java/com/coloop/agent/runtime/` - 运行时核心
- `src/main/java/com/coloop/agent/capability/` - 能力实现

## 测试要求
- 所有新功能需要编写单元测试
- 使用 JUnit 5 测试框架
