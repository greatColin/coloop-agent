"""消息队列

@developer colin.cheng.ai
"""

import asyncio
from typing import Optional
from .message import Message


class MessageQueue:
    """异步消息队列，支持并发接收和顺序处理"""

    def __init__(self):
        self._queue: asyncio.Queue[Message] = asyncio.Queue()

    async def put(self, message: Message) -> None:
        await self._queue.put(message)

    async def get(self) -> Message:
        return await self._queue.get()

    async def get_if_any(self) -> Optional[Message]:
        try:
            return self._queue.get_nowait()
        except asyncio.QueueEmpty:
            return None

    @property
    def empty(self) -> bool:
        return self._queue.empty()
