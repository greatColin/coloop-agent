"""微信平台实现

开发者: colin.cheng.ai
"""

import asyncio
import logging
from typing import Callable, Awaitable, Optional
from ...gateway.platform import Platform
from ...core.message import Message
from .ilink import ILinkClient

logger = logging.getLogger(__name__)


class WeChatPlatform(Platform):
    def __init__(self, token: str, api_base: str = "https://ilinkai.weixin.qq.com"):
        self.token = token
        self.api_base = api_base
        self._client: Optional[ILinkClient] = None
        self._handler: Optional[Callable[[Message], Awaitable[None]]] = None
        self._connected = False
        self._poll_task: Optional[asyncio.Task] = None

    async def connect(self) -> None:
        self._client = ILinkClient(self.token, self.api_base)
        if not await self._client.verify():
            raise Exception("iLink token 无效")
        self._connected = True
        self._poll_task = asyncio.create_task(self._poll_loop())

    async def disconnect(self) -> None:
        self._connected = False
        if self._poll_task:
            self._poll_task.cancel()
            try:
                await self._poll_task
            except asyncio.CancelledError:
                pass
        if self._client:
            await self._client.close()

    async def send_message(self, chat_id: str, content: str) -> bool:
        if not self._client:
            return False
        return await self._client.send_message(chat_id, content)

    async def send_typing(self, chat_id: str) -> bool:
        if not self._client:
            return False
        return await self._client.send_typing(chat_id)

    def get_name(self) -> str:
        return "wechat"

    def is_connected(self) -> bool:
        return self._connected

    async def _poll_loop(self) -> None:
        while self._connected:
            try:
                messages = await self._client.get_messages()
                for data in messages:
                    msg = self._convert(data)
                    if msg and self._handler:
                        await self._handler(msg)
                await asyncio.sleep(1)
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"轮询出错: {e}")
                await asyncio.sleep(5)

    def _convert(self, data: dict) -> Optional[Message]:
        try:
            return Message(
                msg_id=data.get("msg_id", ""),
                platform="wechat",
                user_id=data.get("from_user", ""),
                chat_id=data.get("from_user", ""),
                content=data.get("content", ""),
                msg_type=data.get("type", "text"),
                raw=data,
            )
        except Exception as e:
            logger.error(f"转换消息失败: {e}")
            return None