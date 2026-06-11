# Channel 模块设计文档

## 1. 概述

### 1.1 项目目标

构建一个独立的远程控制模块，支持通过微信等渠道与 AI Agent 交互。参考 hermes 的 Consumer-Dispatch 架构，实现消息的并发接收、队列管理和流式响应。

### 1.2 核心需求

1. **多渠道支持**：目前支持微信（iLink API），预留其他渠道扩展
2. **并发接收**：线程池接收用户消息，Agent 只处理最新或队列中的消息
3. **多次回复**：一轮对话可返回多条消息（思考、工具调用、最终结果）
4. **队列管理**：一轮结束后取队列第一条继续处理
5. **Agent 抽象**：预留接口，支持接入不同 AI 项目

### 1.3 技术选型

- **语言**：Python 3.10+
- **异步框架**：asyncio
- **架构模式**：Consumer-Dispatch（参考 hermes）

---

## 2. 架构设计

### 2.1 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                      Gateway 层                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │   WeChat    │  │   Future    │  │   Future    │     │
│  │   Platform  │  │   Platform  │  │   Platform  │     │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘     │
│         └────────────────┼────────────────┘             │
│                          ▼                               │
│              ┌───────────────────┐                      │
│              │  Platform Registry │                      │
│              └─────────┬─────────┘                      │
│                        ▼                                 │
│  ┌─────────────────────────────────────────────────┐   │
│  │              Stream Layer                         │   │
│  │  Consumer ──▶ Dispatch ──▶ Session               │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                      Session 层                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │   Message   │  │   Agent     │  │   Response  │     │
│  │   Queue     │◀▶│   Strategy  │──▶│   Sender    │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
└─────────────────────────────────────────────────────────┘
```

### 2.2 数据流

```
用户消息 ──▶ Platform ──▶ Consumer ──▶ Dispatch ──▶ Session
                                                      │
                                                      ▼
                                              ┌──────────────┐
                                              │ is_processing?│
                                              └──────┬───────┘
                                                     │
                              ┌───────────────────────┼───────────────────────┐
                              │ Yes                   │ No                    │
                              ▼                       ▼                       │
                    ┌─────────────────┐    ┌─────────────────┐              │
                    │ queue.put(msg)  │    │ agent.stream()  │              │
                    │ return          │    │ send replies    │              │
                    └─────────────────┘    └────────┬────────┘              │
                                                    │                        │
                                                    ▼                        │
                                           ┌─────────────────┐              │
                                           │ queue.get_if_any│              │
                                           └────────┬────────┘              │
                                                    │                        │
                                          ┌─────────┴─────────┐             │
                                          │ has message?       │             │
                                          └─────────┬─────────┘             │
                                          Yes       │       No              │
                                          ▼         │       ▼               │
                                   ┌──────────┐    │  ┌──────────┐         │
                                   │ continue │    │  │  idle    │         │
                                   └──────────┘    │  └──────────┘         │
                                                   │                        │
                                                   └────────────────────────┘
```

---

## 3. 核心组件

### 3.1 消息模型（Message）

```python
@dataclass
class Message:
    msg_id: str                      # 消息 ID
    platform: str                    # 平台标识
    user_id: str                     # 用户 ID
    chat_id: str                     # 会话 ID
    content: str                     # 文本内容
    msg_type: str = "text"           # 消息类型
    media_url: Optional[str] = None  # 媒体 URL
    media_key: Optional[str] = None  # 媒体密钥
    timestamp: float = time.time()   # 时间戳
    raw: Optional[Dict] = None       # 原始数据
```

### 3.2 消息队列（MessageQueue）

```python
class MessageQueue:
    async def put(self, message: Message) -> None
    async def get(self) -> Message
    async def get_if_any(self) -> Optional[Message]
    @property
    def empty(self) -> bool
```

### 3.3 会话管理（Session）

```python
class Session:
    def __init__(self, chat_id, agent, send_message)
    async def on_message(self, message: Message) -> None
    def stop(self) -> None
```

核心逻辑：
- `is_processing` 时：消息入队，返回"收到，稍等..."
- 非处理时：调用 `agent.stream()`，支持多次回复
- 一轮结束后：`queue.get_if_any()` 取队列第一条继续

### 3.4 Agent 策略接口（AgentStrategy）

```python
class AgentStrategy(ABC):
    @abstractmethod
    async def stream(self, message: str) -> AsyncGenerator[str, None]
```

实现示例：
- `TestStrategy`：测试用，返回模拟数据
- `ColoopStrategy`：接入 coloop-agent
- `ShellStrategy`：通过 shell 调用其他软件

---

## 4. 项目结构

```
coloop-agent-channel/
├── pyproject.toml
├── config.yaml
├── src/
│   └── channel/
│       ├── __init__.py
│       ├── core/
│       │   ├── __init__.py
│       │   ├── message.py          # 消息模型
│       │   ├── queue.py            # 消息队列
│       │   └── session.py          # 会话管理
│       ├── gateway/
│       │   ├── __init__.py
│       │   ├── platform.py         # 平台接口
│       │   ├── registry.py         # 平台注册表
│       │   ├── consumer.py         # 消息消费者
│       │   └── dispatch.py         # 消息分发器
│       ├── platform/
│       │   ├── __init__.py
│       │   └── wechat/
│       │       ├── __init__.py
│       │       ├── ilink.py        # iLink 客户端
│       │       └── wechat.py       # 微信平台实现
│       ├── agent/
│       │   ├── __init__.py
│       │   ├── strategy.py         # Agent 接口
│       │   └── test.py             # 测试策略
│       └── cli/
│           ├── __init__.py
│           └── app.py              # CLI 入口
├── tests/
└── docs/
```

---

## 5. 配置示例

```yaml
# config.yaml

wechat:
  enabled: true
  token: "your-ilink-token"
  api_base: "https://ilinkai.weixin.qq.com"

agent:
  strategy: "test"
  test:
    delay: 1.0
```

---

## 6. 微信 iLink API

> **注意**：以下 API 接口为基于 hermesclaw 项目的推测，实际接口需验证。

### 6.1 认证

使用 iLink token 认证，从 hermes 或 openclaw 配置中提取。

### 6.2 消息接收

轮询获取新消息（具体路径待验证）。

### 6.3 消息发送

发送消息（具体路径待验证）。

### 6.4 输入状态

发送"正在输入"状态（具体路径待验证）。

### 6.5 参考资源

- hermesclaw 项目：https://github.com/AaronWong1999/hermesclaw
- iLink API 地址：https://ilinkai.weixin.qq.com

---

## 7. 后续扩展

1. **接入 coloop-agent**：实现 `ColoopStrategy`，通过 Java 进程或 API 调用
2. **接入 Claude Code**：实现 `ShellStrategy`，通过 shell 调用 `claude` 命令
3. **更多渠道**：实现 Telegram、Discord 等平台
4. **持久化**：会话和消息持久化存储
5. **权限控制**：用户白名单、命令权限
