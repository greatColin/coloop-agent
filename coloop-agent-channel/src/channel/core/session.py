"""会话管理

@developer colin.cheng.ai
"""

import asyncio
import logging
from typing import Callable, Awaitable
from .message import Message
from .queue import MessageQueue
from ..agent.strategy import AgentStrategy

logger = logging.getLogger(__name__)


class Session:
    """用户会话，控制消息队列和 Agent 执行流程"""

    def __init__(
        self,
        chat_id: str,
        agent: AgentStrategy,
        send_message: Callable[[str, str], Awaitable[None]],
    ):
        self.chat_id = chat_id
        self.agent = agent
        self.send_message = send_message
        self.queue = MessageQueue()
        self.is_processing = False
        self._stop = False

    async def on_message(self, message: Message) -> None:
        if self.is_processing:
            await self.queue.put(message)
            await self.send_message(self.chat_id, "收到，稍等...")
            return

        self.is_processing = True
        self._stop = False
        try:
            await self._run(message)
        except Exception as e:
            await self.send_message(self.chat_id, f"出错了: {e}")
        finally:
            self.is_processing = False

    async def _run(self, message: Message) -> None:
        current = message
        while current and not self._stop:
            async for reply in self.agent.stream(current.content):
                if self._stop:
                    break
                await self.send_message(self.chat_id, reply)
            current = await self.queue.get_if_any()

    def stop(self) -> None:
        self._stop = True
