# 聊天主题展示页面实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建一个独立 HTML 页面，展示 8 种风格的聊天 UI 主题 Demo，供挑选。

**Architecture:** 单文件纯前端方案，使用 CSS 类作用域隔离各主题样式（`.theme-{name}` 前缀），零 JS 依赖（仅卡片折叠用 10 行内联 JS）。所有主题共享同一套 HTML 结构，通过父容器类名切换样式。

**Tech Stack:** HTML5 + CSS3，Google Fonts CDN 引入特色字体。

---

## 文件结构

| 文件 | 操作 | 说明 |
|------|------|------|
| `coloop-agent-server/src/main/resources/static/theme-showcase.html` | 创建 | 唯一输出文件，包含全部 8 个主题 |

---

### Task 1: HTML 骨架 + 导航 + 共享演示数据

**Files:**
- Create: `coloop-agent-server/src/main/resources/static/theme-showcase.html`

- [ ] **Step 1: 写入基础 HTML 骨架**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>coloop-agent 主题选择器</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <!-- Google Fonts will be added per theme in Task 3 -->
    <style>
        /* Reset and base styles - see Step 2 */
    </style>
</head>
<body>
    <!-- Navigation -->
    <!-- 8 Theme Demos -->
    <script>
        // Collapsible card toggle - see Task 2
    </script>
</body>
</html>
```

- [ ] **Step 2: 写入页面级基础 CSS**

```css
* { margin: 0; padding: 0; box-sizing: border-box; }

body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: #f8f9fa;
    color: #111;
    line-height: 1.6;
}

.page-header {
    position: sticky;
    top: 0;
    z-index: 100;
    background: rgba(255,255,255,0.95);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid #e5e7eb;
    padding: 16px 24px;
}

.page-header h1 {
    font-size: 20px;
    font-weight: 700;
    margin-bottom: 12px;
}

.theme-nav {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.theme-nav a {
    display: inline-block;
    padding: 6px 14px;
    border-radius: 20px;
    background: #f3f4f6;
    color: #374151;
    text-decoration: none;
    font-size: 13px;
    font-weight: 500;
    transition: all 0.2s;
}

.theme-nav a:hover {
    background: #e5e7eb;
}

.theme-section {
    padding: 40px 24px;
    border-bottom: 2px dashed #e5e7eb;
}

.theme-section h2 {
    font-size: 18px;
    font-weight: 700;
    margin-bottom: 4px;
}

.theme-desc {
    font-size: 13px;
    color: #6b7280;
    margin-bottom: 20px;
}

.chat-demo {
    max-width: 800px;
    height: 650px;
    margin: 0 auto;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 20px rgba(0,0,0,0.1);
}
```

- [ ] **Step 3: 写入共享演示数据模板**

每个主题使用相同的 HTML 结构，仅外层类名不同：

```html
<div class="theme-section" id="theme-claude">
    <h2>1. Claude 优雅紫</h2>
    <p class="theme-desc">参考 Claude.ai，温暖优雅，细腻渐变</p>
    <div class="chat-demo theme-claude">
        <!-- Shared chat structure -->
    </div>
</div>
```

共享聊天结构包含：
- `.demo-header`：标题栏 + 连接状态
- `.demo-messages`：消息列表容器（flex 纵向，overflow-y: auto）
- 8 条消息按顺序：user → loop-start → thinking-card → tool-call-card → tool-result-card → assistant → system → input-area

每条消息的 HTML 结构：
```html
<!-- User -->
<div class="msg msg-user">
    <div class="msg-bubble">分析一下这个项目的代码结构</div>
</div>

<!-- Loop Start -->
<div class="msg msg-loop">▶ Attempt 1...</div>

<!-- Thinking Card -->
<div class="card card-thinking">
    <div class="card-header">
        <span class="card-title">💭 Thinking</span>
        <span class="card-toggle">▶</span>
    </div>
    <div class="card-preview">分析代码结构的需求...</div>
    <div class="card-body collapsed">[REASONING]...</div>
</div>

<!-- Tool Call -->
<div class="card card-tool-call">
    <div class="card-header">
        <span class="card-title">🔧 list_directory</span>
        <span class="card-toggle">▶</span>
    </div>
    <div class="card-preview">Args: path=/project</div>
    <div class="card-body collapsed">Name: list_directory\nArgs:...</div>
</div>

<!-- Tool Result -->
<div class="card card-tool-result">
    <div class="card-header">
        <span class="card-title">✅ Result: list_directory</span>
        <span class="card-toggle">▶</span>
    </div>
    <div class="card-preview">core/ capability/ runtime/ entry/</div>
    <div class="card-body collapsed">core/\ncapability/\nruntime/\nentry/</div>
</div>

<!-- Assistant -->
<div class="msg msg-assistant">
    <div class="msg-bubble">这是一个 Maven 多模块项目...</div>
</div>

<!-- System -->
<div class="msg msg-system">已保存到会话历史</div>

<!-- Input Area -->
<div class="input-area">
    <div class="input-box">输入消息... (Shift+Enter 换行, Enter 发送)</div>
    <button class="send-btn">发送</button>
</div>
```

- [ ] **Step 4: 复制此结构 8 次，分别包裹在 `.theme-{name}` 中**

8 个主题类名：`theme-claude`, `theme-chatgpt`, `theme-cursor`, `theme-discord`, `theme-linear`, `theme-telegram`, `theme-terminal`, `theme-glass`

- [ ] **Step 5: Commit**

```bash
git add coloop-agent-server/src/main/resources/static/theme-showcase.html
git commit -m "feat(theme): 主题展示页面骨架 + 导航 + 8 套共享演示数据"
```

---

### Task 2: 卡片折叠交互 JS

**Files:**
- Modify: `coloop-agent-server/src/main/resources/static/theme-showcase.html`（`<script>` 标签内）

- [ ] **Step 1: 写入卡片折叠逻辑**

```javascript
document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.card-header').forEach(function(header) {
        header.addEventListener('click', function() {
            var card = header.parentElement;
            var body = card.querySelector('.card-body');
            var preview = card.querySelector('.card-preview');
            var toggle = header.querySelector('.card-toggle');
            var isCollapsed = body.classList.contains('collapsed');
            if (isCollapsed) {
                body.classList.remove('collapsed');
                if (preview) preview.classList.add('collapsed');
                toggle.textContent = '▼';
            } else {
                body.classList.add('collapsed');
                if (preview) preview.classList.remove('collapsed');
                toggle.textContent = '▶';
            }
        });
    });
});
```

- [ ] **Step 2: 在浏览器打开验证**

用浏览器打开 `coloop-agent-server/src/main/resources/static/theme-showcase.html`，点击任意 Thinking/Tool 卡片的 header，验证展开/折叠功能正常。

- [ ] **Step 3: Commit**

```bash
git add coloop-agent-server/src/main/resources/static/theme-showcase.html
git commit -m "feat(theme): 卡片展开/折叠交互"
```

---

### Task 3: 主题 T1-T4 样式实现

**Files:**
- Modify: `coloop-agent-server/src/main/resources/static/theme-showcase.html`（`<style>` 标签内追加）

- [ ] **Step 1: 添加 Google Fonts 链接到 `<head>`**

```html
<link href="https://fonts.googleapis.com/css2?family=Crimson+Pro:ital,wght@0,400;0,600;1,400&family=IBM+Plex+Mono:wght@400;500&family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&family=Space+Grotesk:wght@400;500;600;700&display=swap" rel="stylesheet">
```

- [ ] **Step 2: 实现 T1 Claude 优雅紫**

```css
.theme-claude {
    font-family: 'Crimson Pro', Georgia, serif;
    background: linear-gradient(135deg, #faf8f5 0%, #f0e6f6 100%);
}
.theme-claude .demo-header {
    padding: 14px 20px;
    background: rgba(255,255,255,0.8);
    border-bottom: 1px solid #e6d5f0;
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.theme-claude .demo-header h3 {
    font-size: 15px;
    font-weight: 600;
    color: #4a306d;
}
.theme-claude .demo-status {
    font-size: 11px;
    padding: 3px 10px;
    border-radius: 12px;
    background: #d4edda;
    color: #155724;
}
.theme-claude .demo-messages {
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 14px;
    height: calc(100% - 110px);
    overflow-y: auto;
}
.theme-claude .msg-user {
    align-self: flex-end;
    max-width: 80%;
}
.theme-claude .msg-user .msg-bubble {
    background: linear-gradient(135deg, #7c3aed 0%, #a78bfa 100%);
    color: #fff;
    padding: 12px 18px;
    border-radius: 18px 18px 4px 18px;
    font-size: 14px;
    line-height: 1.6;
    box-shadow: 0 2px 8px rgba(124,58,237,0.25);
}
.theme-claude .msg-assistant {
    align-self: flex-start;
    max-width: 85%;
}
.theme-claude .msg-assistant .msg-bubble {
    background: #fff;
    color: #374151;
    padding: 14px 18px;
    border-radius: 18px 18px 18px 4px;
    font-size: 14px;
    line-height: 1.7;
    box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.theme-claude .msg-loop,
.theme-claude .msg-system {
    align-self: center;
    font-size: 12px;
    color: #9ca3af;
    padding: 4px 12px;
}
.theme-claude .card {
    max-width: 90%;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.theme-claude .card-thinking {
    background: #faf9f7;
    border: 1px solid #e6ddd0;
}
.theme-claude .card-thinking .card-header {
    background: #f0ebe4;
    color: #6b5b4e;
}
.theme-claude .card-tool-call {
    background: #fef9e7;
    border: 1px solid #f5d76e;
}
.theme-claude .card-tool-call .card-header {
    background: #f5e6a3;
    color: #7d5c00;
}
.theme-claude .card-tool-result {
    background: #f0fdf4;
    border: 1px solid #86efac;
}
.theme-claude .card-tool-result .card-header {
    background: #bbf7d0;
    color: #166534;
}
.theme-claude .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 14px;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    user-select: none;
}
.theme-claude .card-preview {
    padding: 8px 14px;
    font-size: 12px;
    color: #9ca3af;
    border-top: 1px solid rgba(0,0,0,0.04);
}
.theme-claude .card-body {
    padding: 12px 14px;
    font-size: 13px;
    font-family: 'IBM Plex Mono', monospace;
    line-height: 1.5;
    max-height: 200px;
    overflow-y: auto;
    white-space: pre-wrap;
}
.theme-claude .card-body.collapsed,
.theme-claude .card-preview.collapsed {
    display: none;
}
.theme-claude .input-area {
    display: flex;
    gap: 10px;
    padding: 12px 20px;
    background: rgba(255,255,255,0.8);
    border-top: 1px solid #e6d5f0;
}
.theme-claude .input-box {
    flex: 1;
    padding: 10px 16px;
    background: #fff;
    border: 1px solid #ddd6fe;
    border-radius: 20px;
    font-size: 13px;
    color: #9ca3af;
}
.theme-claude .send-btn {
    padding: 10px 20px;
    background: linear-gradient(135deg, #7c3aed 0%, #a78bfa 100%);
    color: #fff;
    border: none;
    border-radius: 20px;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
}
```

- [ ] **Step 3: 实现 T2 ChatGPT 极简**

```css
.theme-chatgpt {
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
    background: #ffffff;
}
.theme-chatgpt .demo-header {
    padding: 12px 20px;
    border-bottom: 1px solid #e5e5e5;
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.theme-chatgpt .demo-header h3 {
    font-size: 14px;
    font-weight: 600;
    color: #111;
}
.theme-chatgpt .demo-status {
    font-size: 11px;
    padding: 3px 10px;
    border-radius: 12px;
    background: #e6f4ea;
    color: #1e8e3e;
}
.theme-chatgpt .demo-messages {
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    height: calc(100% - 108px);
    overflow-y: auto;
    background: #fff;
}
.theme-chatgpt .msg-user {
    align-self: flex-end;
    max-width: 80%;
}
.theme-chatgpt .msg-user .msg-bubble {
    background: #f4f4f4;
    color: #111;
    padding: 10px 16px;
    border-radius: 16px;
    font-size: 14px;
    line-height: 1.5;
}
.theme-chatgpt .msg-assistant {
    align-self: flex-start;
    max-width: 85%;
}
.theme-chatgpt .msg-assistant .msg-bubble {
    color: #111;
    padding: 0;
    font-size: 15px;
    line-height: 1.6;
}
.theme-chatgpt .msg-loop,
.theme-chatgpt .msg-system {
    align-self: center;
    font-size: 12px;
    color: #999;
}
.theme-chatgpt .card {
    max-width: 90%;
    border-radius: 8px;
    border: 1px solid #e5e5e5;
    overflow: hidden;
}
.theme-chatgpt .card-thinking { background: #fafafa; }
.theme-chatgpt .card-thinking .card-header { background: #f5f5f5; color: #666; }
.theme-chatgpt .card-tool-call { background: #fffbeb; border-color: #fcd34d; }
.theme-chatgpt .card-tool-call .card-header { background: #fef3c7; color: #92400e; }
.theme-chatgpt .card-tool-result { background: #f0fdf4; border-color: #86efac; }
.theme-chatgpt .card-tool-result .card-header { background: #dcfce7; color: #166534; }
.theme-chatgpt .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    user-select: none;
}
.theme-chatgpt .card-preview {
    padding: 6px 12px;
    font-size: 12px;
    color: #999;
    border-top: 1px solid #eee;
}
.theme-chatgpt .card-body {
    padding: 10px 12px;
    font-size: 13px;
    font-family: 'JetBrains Mono', monospace;
    line-height: 1.5;
    max-height: 200px;
    overflow-y: auto;
    white-space: pre-wrap;
}
.theme-chatgpt .card-body.collapsed,
.theme-chatgpt .card-preview.collapsed { display: none; }
.theme-chatgpt .input-area {
    display: flex;
    gap: 8px;
    padding: 12px 20px;
    border-top: 1px solid #e5e5e5;
    background: #fff;
}
.theme-chatgpt .input-box {
    flex: 1;
    padding: 10px 14px;
    border: 1px solid #d1d5db;
    border-radius: 8px;
    font-size: 14px;
    color: #9ca3af;
}
.theme-chatgpt .send-btn {
    padding: 10px 20px;
    background: #19c37d;
    color: #fff;
    border: none;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
}
```

- [ ] **Step 4: 实现 T3 Cursor 极客暗**

```css
.theme-cursor {
    font-family: 'JetBrains Mono', 'IBM Plex Mono', monospace;
    background: #0f172a;
    color: #e2e8f0;
}
.theme-cursor .demo-header {
    padding: 12px 20px;
    background: #1e293b;
    border-bottom: 1px solid #334155;
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.theme-cursor .demo-header h3 {
    font-size: 13px;
    font-weight: 600;
    color: #94a3b8;
    text-transform: uppercase;
    letter-spacing: 0.5px;
}
.theme-cursor .demo-status {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 4px;
    background: #064e3b;
    color: #34d399;
    font-family: 'JetBrains Mono', monospace;
}
.theme-cursor .demo-messages {
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 14px;
    height: calc(100% - 108px);
    overflow-y: auto;
    background: #0f172a;
}
.theme-cursor .msg-user {
    align-self: flex-end;
    max-width: 80%;
}
.theme-cursor .msg-user .msg-bubble {
    background: #1e40af;
    color: #fff;
    padding: 10px 14px;
    border-radius: 6px;
    font-size: 13px;
    line-height: 1.5;
    border: 1px solid #3b82f6;
}
.theme-cursor .msg-assistant {
    align-self: flex-start;
    max-width: 85%;
}
.theme-cursor .msg-assistant .msg-bubble {
    color: #e2e8f0;
    padding: 0;
    font-size: 13px;
    line-height: 1.6;
}
.theme-cursor .msg-loop,
.theme-cursor .msg-system {
    align-self: center;
    font-size: 11px;
    color: #64748b;
    font-family: 'JetBrains Mono', monospace;
}
.theme-cursor .card {
    max-width: 90%;
    border-radius: 6px;
    border: 1px solid #334155;
    overflow: hidden;
    font-size: 12px;
}
.theme-cursor .card-thinking {
    background: #1e293b;
    border-color: #475569;
}
.theme-cursor .card-thinking .card-header {
    background: #334155;
    color: #94a3b8;
}
.theme-cursor .card-tool-call {
    background: #292524;
    border-color: #f59e0b;
}
.theme-cursor .card-tool-call .card-header {
    background: #451a03;
    color: #fbbf24;
}
.theme-cursor .card-tool-result {
    background: #064e3b;
    border-color: #10b981;
}
.theme-cursor .card-tool-result .card-header {
    background: #065f46;
    color: #34d399;
}
.theme-cursor .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 6px 12px;
    font-size: 11px;
    font-weight: 500;
    cursor: pointer;
    user-select: none;
    font-family: 'JetBrains Mono', monospace;
}
.theme-cursor .card-preview {
    padding: 6px 12px;
    font-size: 11px;
    color: #64748b;
    border-top: 1px solid #334155;
    font-family: 'JetBrains Mono', monospace;
}
.theme-cursor .card-body {
    padding: 10px 12px;
    font-size: 12px;
    line-height: 1.5;
    max-height: 200px;
    overflow-y: auto;
    white-space: pre-wrap;
    color: #cbd5e1;
    font-family: 'JetBrains Mono', monospace;
}
.theme-cursor .card-body.collapsed,
.theme-cursor .card-preview.collapsed { display: none; }
.theme-cursor .input-area {
    display: flex;
    gap: 8px;
    padding: 12px 20px;
    background: #1e293b;
    border-top: 1px solid #334155;
}
.theme-cursor .input-box {
    flex: 1;
    padding: 8px 12px;
    background: #0f172a;
    border: 1px solid #475569;
    border-radius: 6px;
    font-size: 13px;
    color: #64748b;
    font-family: 'JetBrains Mono', monospace;
}
.theme-cursor .send-btn {
    padding: 8px 16px;
    background: #2563eb;
    color: #fff;
    border: none;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    font-family: 'JetBrains Mono', monospace;
}
```

- [ ] **Step 5: 实现 T4 Discord 社交暗**

```css
.theme-discord {
    font-family: 'Inter', 'Helvetica Neue', sans-serif;
    background: #36393f;
    color: #dcddde;
}
.theme-discord .demo-header {
    padding: 12px 20px;
    background: #202225;
    border-bottom: 1px solid #202225;
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.theme-discord .demo-header h3 {
    font-size: 15px;
    font-weight: 600;
    color: #fff;
}
.theme-discord .demo-status {
    font-size: 12px;
    padding: 3px 10px;
    border-radius: 12px;
    background: #3ba55d;
    color: #fff;
    font-weight: 500;
}
.theme-discord .demo-messages {
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    height: calc(100% - 108px);
    overflow-y: auto;
    background: #36393f;
}
.theme-discord .msg-user {
    align-self: flex-end;
    max-width: 80%;
}
.theme-discord .msg-user .msg-bubble {
    background: #5865f2;
    color: #fff;
    padding: 10px 14px;
    border-radius: 16px;
    font-size: 14px;
    line-height: 1.5;
}
.theme-discord .msg-assistant {
    align-self: flex-start;
    max-width: 85%;
}
.theme-discord .msg-assistant .msg-bubble {
    color: #dcddde;
    padding: 0;
    font-size: 14px;
    line-height: 1.6;
}
.theme-discord .msg-loop,
.theme-discord .msg-system {
    align-self: center;
    font-size: 12px;
    color: #72767d;
}
.theme-discord .card {
    max-width: 90%;
    border-radius: 8px;
    overflow: hidden;
    font-size: 13px;
}
.theme-discord .card-thinking {
    background: #2f3136;
    border: 1px solid #40444b;
}
.theme-discord .card-thinking .card-header {
    background: #202225;
    color: #b9bbbe;
}
.theme-discord .card-tool-call {
    background: #2f3136;
    border: 1px solid #faa81a;
}
.theme-discord .card-tool-call .card-header {
    background: rgba(250,168,26,0.15);
    color: #faa81a;
}
.theme-discord .card-tool-result {
    background: #2f3136;
    border: 1px solid #3ba55d;
}
.theme-discord .card-tool-result .card-header {
    background: rgba(59,165,93,0.15);
    color: #3ba55d;
}
.theme-discord .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 14px;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    user-select: none;
}
.theme-discord .card-preview {
    padding: 6px 14px;
    font-size: 12px;
    color: #72767d;
    border-top: 1px solid #40444b;
}
.theme-discord .card-body {
    padding: 10px 14px;
    font-size: 13px;
    line-height: 1.5;
    max-height: 200px;
    overflow-y: auto;
    white-space: pre-wrap;
    color: #dcddde;
    font-family: 'JetBrains Mono', monospace;
}
.theme-discord .card-body.collapsed,
.theme-discord .card-preview.collapsed { display: none; }
.theme-discord .input-area {
    display: flex;
    gap: 10px;
    padding: 12px 20px;
    background: #40444b;
    border-top: 1px solid #202225;
}
.theme-discord .input-box {
    flex: 1;
    padding: 10px 14px;
    background: #40444b;
    border: none;
    border-radius: 8px;
    font-size: 14px;
    color: #72767d;
}
.theme-discord .send-btn {
    padding: 10px 20px;
    background: #5865f2;
    color: #fff;
    border: none;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
}
```

- [ ] **Step 6: 浏览器验证 T1-T4**

打开页面，检查：
- 导航链接可点击跳转
- Claude 主题有紫色调和渐变
- ChatGPT 主题为白底绿按钮
- Cursor 主题为深蓝灰底等宽字体
- Discord 主题为深灰底彩色标签
- 所有卡片的展开/折叠正常

- [ ] **Step 7: Commit**

```bash
git add coloop-agent-server/src/main/resources/static/theme-showcase.html
git commit -m "feat(theme): T1-T4 主题样式实现 (Claude/ChatGPT/Cursor/Discord)"
```

---

### Task 4: 主题 T5-T8 样式实现

**Files:**
- Modify: `coloop-agent-server/src/main/resources/static/theme-showcase.html`（`<style>` 标签内追加）

- [ ] **Step 1: 实现 T5 Linear 专业白**

```css
.theme-linear {
    font-family: 'Inter', -apple-system, sans-serif;
    background: #fff;
    color: #111;
}
.theme-linear .demo-header {
    padding: 16px 24px;
    border-bottom: 1px solid #e6e6e6;
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.theme-linear .demo-header h3 {
    font-size: 14px;
    font-weight: 600;
    color: #111;
    letter-spacing: -0.2px;
}
.theme-linear .demo-status {
    font-size: 11px;
    padding: 3px 10px;
    border-radius: 4px;
    background: #eef2ff;
    color: #4f46e5;
    font-weight: 500;
}
.theme-linear .demo-messages {
    padding: 24px;
    display: flex;
    flex-direction: column;
    gap: 20px;
    height: calc(100% - 116px);
    overflow-y: auto;
    background: #fff;
}
.theme-linear .msg-user {
    align-self: flex-end;
    max-width: 75%;
}
.theme-linear .msg-user .msg-bubble {
    background: #111;
    color: #fff;
    padding: 12px 18px;
    border-radius: 12px;
    font-size: 14px;
    line-height: 1.5;
}
.theme-linear .msg-assistant {
    align-self: flex-start;
    max-width: 80%;
}
.theme-linear .msg-assistant .msg-bubble {
    color: #111;
    padding: 0;
    font-size: 15px;
    line-height: 1.6;
}
.theme-linear .msg-loop,
.theme-linear .msg-system {
    align-self: center;
    font-size: 12px;
    color: #999;
    letter-spacing: 0.3px;
}
.theme-linear .card {
    max-width: 85%;
    border-radius: 8px;
    border: 1px solid #e6e6e6;
    overflow: hidden;
}
.theme-linear .card-thinking { background: #fafafa; }
.theme-linear .card-thinking .card-header { background: #f5f5f5; color: #666; }
.theme-linear .card-tool-call { background: #fffbeb; border-color: #f5e050; }
.theme-linear .card-tool-call .card-header { background: #fef9c3; color: #854d0e; }
.theme-linear .card-tool-result { background: #f0fdf4; border-color: #86efac; }
.theme-linear .card-tool-result .card-header { background: #dcfce7; color: #166534; }
.theme-linear .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 14px;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    user-select: none;
}
.theme-linear .card-preview {
    padding: 6px 14px;
    font-size: 12px;
    color: #999;
    border-top: 1px solid #eee;
}
.theme-linear .card-body {
    padding: 12px 14px;
    font-size: 13px;
    line-height: 1.5;
    max-height: 200px;
    overflow-y: auto;
    white-space: pre-wrap;
    font-family: 'JetBrains Mono', monospace;
}
.theme-linear .card-body.collapsed,
.theme-linear .card-preview.collapsed { display: none; }
.theme-linear .input-area {
    display: flex;
    gap: 10px;
    padding: 14px 24px;
    border-top: 1px solid #e6e6e6;
    background: #fafafa;
}
.theme-linear .input-box {
    flex: 1;
    padding: 10px 14px;
    background: #fff;
    border: 1px solid #e6e6e6;
    border-radius: 8px;
    font-size: 14px;
    color: #999;
}
.theme-linear .send-btn {
    padding: 10px 20px;
    background: #111;
    color: #fff;
    border: none;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
}
```

- [ ] **Step 2: 实现 T6 Telegram 明快**

```css
.theme-telegram {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: linear-gradient(180deg, #6ab4f0 0%, #5a9fd6 60%, #4a8bc0 100%);
    color: #111;
}
.theme-telegram .demo-header {
    padding: 10px 20px;
    background: rgba(255,255,255,0.95);
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.theme-telegram .demo-header h3 {
    font-size: 15px;
    font-weight: 600;
    color: #111;
}
.theme-telegram .demo-status {
    font-size: 12px;
    padding: 3px 10px;
    border-radius: 12px;
    background: #e8f5e9;
    color: #2e7d32;
    font-weight: 500;
}
.theme-telegram .demo-messages {
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    height: calc(100% - 104px);
    overflow-y: auto;
}
.theme-telegram .msg-user {
    align-self: flex-end;
    max-width: 80%;
}
.theme-telegram .msg-user .msg-bubble {
    background: #effdde;
    color: #111;
    padding: 10px 14px;
    border-radius: 18px 18px 4px 18px;
    font-size: 14px;
    line-height: 1.5;
    box-shadow: 0 1px 2px rgba(0,0,0,0.1);
}
.theme-telegram .msg-assistant {
    align-self: flex-start;
    max-width: 85%;
}
.theme-telegram .msg-assistant .msg-bubble {
    background: #fff;
    color: #111;
    padding: 10px 14px;
    border-radius: 18px 18px 18px 4px;
    font-size: 14px;
    line-height: 1.5;
    box-shadow: 0 1px 2px rgba(0,0,0,0.1);
}
.theme-telegram .msg-loop,
.theme-telegram .msg-system {
    align-self: center;
    font-size: 12px;
    color: rgba(255,255,255,0.8);
    text-shadow: 0 1px 2px rgba(0,0,0,0.2);
}
.theme-telegram .card {
    max-width: 90%;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 1px 4px rgba(0,0,0,0.15);
}
.theme-telegram .card-thinking {
    background: rgba(255,255,255,0.95);
    border: none;
}
.theme-telegram .card-thinking .card-header {
    background: #f0f0f0;
    color: #666;
}
.theme-telegram .card-tool-call {
    background: rgba(255,255,255,0.95);
    border: none;
}
.theme-telegram .card-tool-call .card-header {
    background: #fff3e0;
    color: #e65100;
}
.theme-telegram .card-tool-result {
    background: rgba(255,255,255,0.95);
    border: none;
}
.theme-telegram .card-tool-result .card-header {
    background: #e8f5e9;
    color: #2e7d32;
}
.theme-telegram .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 14px;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    user-select: none;
}
.theme-telegram .card-preview {
    padding: 6px 14px;
    font-size: 12px;
    color: #999;
    border-top: 1px solid #eee;
}
.theme-telegram .card-body {
    padding: 10px 14px;
    font-size: 13px;
    line-height: 1.5;
    max-height: 200px;
    overflow-y: auto;
    white-space: pre-wrap;
    font-family: 'JetBrains Mono', monospace;
    background: #fff;
}
.theme-telegram .card-body.collapsed,
.theme-telegram .card-preview.collapsed { display: none; }
.theme-telegram .input-area {
    display: flex;
    gap: 8px;
    padding: 10px 20px;
    background: rgba(255,255,255,0.95);
}
.theme-telegram .input-box {
    flex: 1;
    padding: 10px 14px;
    background: #f0f0f0;
    border: none;
    border-radius: 20px;
    font-size: 14px;
    color: #999;
}
.theme-telegram .send-btn {
    padding: 10px 20px;
    background: #3390ec;
    color: #fff;
    border: none;
    border-radius: 20px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
}
```

- [ ] **Step 3: 实现 T7 终端复古**

```css
.theme-terminal {
    font-family: 'JetBrains Mono', 'Courier New', monospace;
    background: #0c0c0c;
    color: #0f0;
}
.theme-terminal .demo-header {
    padding: 8px 16px;
    background: #1a1a1a;
    border-bottom: 2px solid #0f0;
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.theme-terminal .demo-header h3 {
    font-size: 13px;
    font-weight: 700;
    color: #0f0;
    text-transform: uppercase;
}
.theme-terminal .demo-status {
    font-size: 11px;
    padding: 2px 8px;
    background: #0f0;
    color: #000;
    font-weight: 700;
}
.theme-terminal .demo-messages {
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    height: calc(100% - 100px);
    overflow-y: auto;
    background: #0c0c0c;
    position: relative;
}
/* Scanline effect */
.theme-terminal .demo-messages::after {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    background: repeating-linear-gradient(
        0deg,
        rgba(0,0,0,0.15),
        rgba(0,0,0,0.15) 1px,
        transparent 1px,
        transparent 2px
    );
    pointer-events: none;
}
.theme-terminal .msg-user {
    align-self: flex-end;
    max-width: 85%;
}
.theme-terminal .msg-user .msg-bubble {
    background: #003300;
    color: #0f0;
    padding: 6px 12px;
    border: 1px solid #0f0;
    font-size: 13px;
    line-height: 1.4;
}
.theme-terminal .msg-user .msg-bubble::before {
    content: '> ';
    color: #0f0;
}
.theme-terminal .msg-assistant {
    align-self: flex-start;
    max-width: 90%;
}
.theme-terminal .msg-assistant .msg-bubble {
    color: #0f0;
    padding: 0;
    font-size: 13px;
    line-height: 1.5;
}
.theme-terminal .msg-loop,
.theme-terminal .msg-system {
    align-self: flex-start;
    font-size: 12px;
    color: #090;
}
.theme-terminal .msg-loop::before,
.theme-terminal .msg-system::before {
    content: '# ';
    color: #0f0;
}
.theme-terminal .card {
    max-width: 95%;
    border: 1px solid #0f0;
    overflow: hidden;
    font-size: 12px;
}
.theme-terminal .card-thinking {
    background: #001100;
}
.theme-terminal .card-thinking .card-header {
    background: #003300;
    color: #0f0;
}
.theme-terminal .card-tool-call {
    background: #1a1000;
    border-color: #ff0;
}
.theme-terminal .card-tool-call .card-header {
    background: #332200;
    color: #ff0;
}
.theme-terminal .card-tool-result {
    background: #001a00;
    border-color: #0f0;
}
.theme-terminal .card-tool-result .card-header {
    background: #003300;
    color: #0f0;
}
.theme-terminal .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 4px 10px;
    font-size: 11px;
    font-weight: 700;
    cursor: pointer;
    user-select: none;
    text-transform: uppercase;
}
.theme-terminal .card-preview {
    padding: 4px 10px;
    font-size: 11px;
    color: #090;
    border-top: 1px solid #030;
}
.theme-terminal .card-body {
    padding: 8px 10px;
    font-size: 12px;
    line-height: 1.4;
    max-height: 200px;
    overflow-y: auto;
    white-space: pre-wrap;
    color: #0c0;
    background: #001100;
}
.theme-terminal .card-body.collapsed,
.theme-terminal .card-preview.collapsed { display: none; }
.theme-terminal .input-area {
    display: flex;
    gap: 8px;
    padding: 10px 16px;
    background: #1a1a1a;
    border-top: 2px solid #0f0;
}
.theme-terminal .input-box {
    flex: 1;
    padding: 6px 12px;
    background: #0c0c0c;
    border: 1px solid #0f0;
    font-size: 13px;
    color: #090;
    font-family: 'JetBrains Mono', monospace;
}
.theme-terminal .send-btn {
    padding: 6px 14px;
    background: #0f0;
    color: #000;
    border: none;
    font-size: 12px;
    font-weight: 700;
    cursor: pointer;
    text-transform: uppercase;
    font-family: 'JetBrains Mono', monospace;
}
```

- [ ] **Step 4: 实现 T8 玻璃华丽**

```css
.theme-glass {
    font-family: 'Space Grotesk', 'Inter', sans-serif;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 25%, #f093fb 50%, #f5576c 75%, #667eea 100%);
    background-size: 400% 400%;
    animation: gradientShift 15s ease infinite;
    color: #fff;
}
@keyframes gradientShift {
    0% { background-position: 0% 50%; }
    50% { background-position: 100% 50%; }
    100% { background-position: 0% 50%; }
}
.theme-glass .demo-header {
    padding: 14px 20px;
    background: rgba(255,255,255,0.1);
    backdrop-filter: blur(20px);
    border-bottom: 1px solid rgba(255,255,255,0.2);
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.theme-glass .demo-header h3 {
    font-size: 15px;
    font-weight: 600;
    color: #fff;
    text-shadow: 0 2px 4px rgba(0,0,0,0.2);
}
.theme-glass .demo-status {
    font-size: 12px;
    padding: 4px 12px;
    border-radius: 20px;
    background: rgba(255,255,255,0.2);
    color: #fff;
    backdrop-filter: blur(10px);
    font-weight: 500;
}
.theme-glass .demo-messages {
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 14px;
    height: calc(100% - 112px);
    overflow-y: auto;
}
.theme-glass .msg-user {
    align-self: flex-end;
    max-width: 80%;
}
.theme-glass .msg-user .msg-bubble {
    background: rgba(255,255,255,0.25);
    backdrop-filter: blur(20px);
    color: #fff;
    padding: 12px 18px;
    border-radius: 20px 20px 4px 20px;
    font-size: 14px;
    line-height: 1.6;
    border: 1px solid rgba(255,255,255,0.3);
    box-shadow: 0 4px 30px rgba(0,0,0,0.1);
}
.theme-glass .msg-assistant {
    align-self: flex-start;
    max-width: 85%;
}
.theme-glass .msg-assistant .msg-bubble {
    background: rgba(255,255,255,0.15);
    backdrop-filter: blur(20px);
    color: #fff;
    padding: 12px 18px;
    border-radius: 20px 20px 20px 4px;
    font-size: 14px;
    line-height: 1.6;
    border: 1px solid rgba(255,255,255,0.25);
    box-shadow: 0 4px 30px rgba(0,0,0,0.1);
}
.theme-glass .msg-loop,
.theme-glass .msg-system {
    align-self: center;
    font-size: 12px;
    color: rgba(255,255,255,0.7);
    text-shadow: 0 1px 2px rgba(0,0,0,0.2);
}
.theme-glass .card {
    max-width: 90%;
    border-radius: 16px;
    overflow: hidden;
    background: rgba(255,255,255,0.1);
    backdrop-filter: blur(20px);
    border: 1px solid rgba(255,255,255,0.2);
    box-shadow: 0 4px 30px rgba(0,0,0,0.1);
}
.theme-glass .card-thinking .card-header {
    background: rgba(255,255,255,0.15);
    color: rgba(255,255,255,0.9);
}
.theme-glass .card-tool-call .card-header {
    background: rgba(255,200,50,0.25);
    color: #ffe066;
}
.theme-glass .card-tool-result .card-header {
    background: rgba(50,255,150,0.2);
    color: #66ffb2;
}
.theme-glass .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 16px;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    user-select: none;
    backdrop-filter: blur(10px);
}
.theme-glass .card-preview {
    padding: 8px 16px;
    font-size: 12px;
    color: rgba(255,255,255,0.6);
    border-top: 1px solid rgba(255,255,255,0.1);
}
.theme-glass .card-body {
    padding: 12px 16px;
    font-size: 13px;
    line-height: 1.5;
    max-height: 200px;
    overflow-y: auto;
    white-space: pre-wrap;
    color: rgba(255,255,255,0.85);
    font-family: 'JetBrains Mono', monospace;
    background: rgba(0,0,0,0.1);
}
.theme-glass .card-body.collapsed,
.theme-glass .card-preview.collapsed { display: none; }
.theme-glass .input-area {
    display: flex;
    gap: 10px;
    padding: 12px 20px;
    background: rgba(255,255,255,0.1);
    backdrop-filter: blur(20px);
    border-top: 1px solid rgba(255,255,255,0.2);
}
.theme-glass .input-box {
    flex: 1;
    padding: 10px 16px;
    background: rgba(255,255,255,0.15);
    backdrop-filter: blur(20px);
    border: 1px solid rgba(255,255,255,0.25);
    border-radius: 20px;
    font-size: 14px;
    color: rgba(255,255,255,0.6);
}
.theme-glass .send-btn {
    padding: 10px 24px;
    background: rgba(255,255,255,0.3);
    backdrop-filter: blur(20px);
    color: #fff;
    border: 1px solid rgba(255,255,255,0.4);
    border-radius: 20px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 2px 10px rgba(0,0,0,0.2);
}
```

- [ ] **Step 5: 浏览器验证 T5-T8**

打开页面，检查：
- Linear：极简黑白，大量留白
- Telegram：蓝渐变背景，白色圆角气泡
- Terminal：黑底绿字，扫描线，硬核风格
- Glass：流动渐变背景，毛玻璃效果
- 所有卡片展开/折叠正常

- [ ] **Step 6: Commit**

```bash
git add coloop-agent-server/src/main/resources/static/theme-showcase.html
git commit -m "feat(theme): T5-T8 主题样式实现 (Linear/Telegram/Terminal/Glass)"
```

---

### Task 5: 最终验证与收尾

**Files:**
- Modify: `coloop-agent-server/src/main/resources/static/theme-showcase.html`

- [ ] **Step 1: 全页面滚动检查**

从顶部到底部滚动整个页面，确认：
- 8 个主题区块之间分隔清晰
- 导航链接正确跳转到对应锚点
- 每个 Demo 高度一致（650px）
- 无水平滚动条
- 移动端视口正常（缩小浏览器宽度检查）

- [ ] **Step 2: 字体加载检查**

确认 Google Fonts 正确加载，各主题使用对应字体：
- Claude：Crimson Pro（衬线）
- ChatGPT / Linear / Discord：Inter（无衬线）
- Cursor / Terminal：JetBrains Mono（等宽）
- Glass：Space Grotesk（几何无衬线）

- [ ] **Step 3: 最终 Commit**

```bash
git add coloop-agent-server/src/main/resources/static/theme-showcase.html
git commit -m "feat(theme): 主题展示页面完成，8 套风格可供挑选"
```

---

## Spec 覆盖检查

| 设计文档要求 | 对应任务 |
|-------------|---------|
| 单文件独立 HTML | Task 1 |
| 8 个主题 | Task 3 (T1-T4), Task 4 (T5-T8) |
| 所有组件类型展示 | Task 1 共享演示数据 |
| 样式作用域隔离 | Task 1 基础 CSS + 各主题前缀 |
| 卡片折叠交互 | Task 2 |
| 导航锚点跳转 | Task 1 |
| Google Fonts | Task 3 |
| 浏览器验证 | 每个 Task 末尾 |

无遗漏。

## 无占位符检查

- 无 "TBD"/"TODO"
- 无 "添加适当的错误处理"
- 所有代码步骤包含完整代码
- 所有命令包含预期输出
- 通过。
