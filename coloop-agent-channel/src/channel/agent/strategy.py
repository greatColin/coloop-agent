"""Agent 策略接口"""

from abc import ABC, abstractmethod
from typing import AsyncGenerator


class AgentStrategy(ABC):
    """Agent 策略抽象接口"""

    @abstractmethod
    async def stream(self, message: str) -> AsyncGenerator[str, None]:
        """流式处理消息，yield 多个回复"""
        ...
        yield  # 使函数成为 generator
