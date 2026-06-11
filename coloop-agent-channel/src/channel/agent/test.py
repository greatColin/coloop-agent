"""测试 Agent 策略"""

import asyncio
from typing import AsyncGenerator
from .strategy import AgentStrategy


class TestStrategy(AgentStrategy):
    """测试用策略，返回模拟数据"""

    def __init__(self, delay: float = 1.0):
        self.delay = delay
        self.call_count = 0

    async def stream(self, message: str) -> AsyncGenerator[str, None]:
        self.call_count += 1
        yield f"🤔 收到消息: {message}"
        await asyncio.sleep(self.delay)
        yield f"🔧 正在处理..."
        await asyncio.sleep(self.delay)
        yield f"✅ 处理完成！您说了: {message}"
