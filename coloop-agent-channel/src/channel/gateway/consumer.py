"""消息消费者

@developer colin.cheng.ai
"""

import asyncio
import logging
from ..core.message import Message
from ..core.queue import MessageQueue

logger = logging.getLogger(__name__)


class StreamConsumer:
    def __init__(self):
        self._queue = MessageQueue()
        self._running = False

    async def start(self) -> None:
        self._running = True

    async def stop(self) -> None:
        self._running = False

    async def on_message(self, message: Message) -> None:
        if not self._running:
            return
        await self._queue.put(message)

    async def get_message(self) -> Message:
        return await self._queue.get()

    async def get_message_if_any(self):
        return await self._queue.get_if_any()
