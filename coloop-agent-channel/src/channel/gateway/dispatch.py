"""消息分发器

@developer colin.cheng.ai
"""

import asyncio
import logging
from typing import Dict
from ..core.message import Message
from ..core.session import Session
from ..agent.strategy import AgentStrategy
from .registry import PlatformRegistry
from .consumer import StreamConsumer

logger = logging.getLogger(__name__)


class StreamDispatch:
    def __init__(self):
        self._sessions: Dict[str, Session] = {}
        self._running = False
        self._task = None

    async def start(self, agent_strategy: AgentStrategy, platform_registry: PlatformRegistry, consumer: StreamConsumer) -> None:
        self._agent = agent_strategy
        self._registry = platform_registry
        self._consumer = consumer
        self._running = True
        self._task = asyncio.create_task(self._dispatch_loop())

    async def stop(self) -> None:
        self._running = False
        if self._task:
            self._task.cancel()
            try:
                await self._task
            except asyncio.CancelledError:
                pass

    async def _dispatch_loop(self) -> None:
        while self._running:
            try:
                message = await self._consumer.get_message()
                await self._dispatch(message)
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"分发消息出错: {e}")

    async def _dispatch(self, message: Message) -> None:
        chat_id = message.chat_id
        if chat_id not in self._sessions:
            platform = self._registry.get(message.platform)
            if not platform:
                return
            session = Session(chat_id=chat_id, agent=self._agent, send_message=platform.send_message)
            self._sessions[chat_id] = session
        await self._sessions[chat_id].on_message(message)
