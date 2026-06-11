"""iLink API 客户端（API 路径为推测，实际使用时需验证）

开发者: colin.cheng.ai
"""

import logging
from typing import Optional, Dict, Any, List
import aiohttp

logger = logging.getLogger(__name__)


class ILinkClient:
    def __init__(self, token: str, api_base: str = "https://ilinkai.weixin.qq.com"):
        self.token = token
        self.api_base = api_base.rstrip("/")
        self._session: Optional[aiohttp.ClientSession] = None
        self._last_msg_id: Optional[str] = None

    async def _get_session(self) -> aiohttp.ClientSession:
        if self._session is None or self._session.closed:
            self._session = aiohttp.ClientSession(headers={"Authorization": f"Bearer {self.token}"})
        return self._session

    async def close(self) -> None:
        if self._session and not self._session.closed:
            await self._session.close()

    async def verify(self) -> bool:
        try:
            session = await self._get_session()
            async with session.get(f"{self.api_base}/api/verify") as resp:
                return resp.status == 200
        except Exception as e:
            logger.error(f"验证失败: {e}")
            return False

    async def get_messages(self) -> List[Dict[str, Any]]:
        try:
            session = await self._get_session()
            params = {}
            if self._last_msg_id:
                params["since"] = self._last_msg_id
            async with session.get(f"{self.api_base}/api/messages", params=params) as resp:
                if resp.status == 200:
                    data = await resp.json()
                    messages = data.get("messages", [])
                    if messages:
                        self._last_msg_id = messages[-1].get("msg_id")
                    return messages
                return []
        except Exception as e:
            logger.error(f"获取消息失败: {e}")
            return []

    async def send_message(self, to_user: str, content: str) -> bool:
        try:
            session = await self._get_session()
            async with session.post(f"{self.api_base}/api/sendmessage", json={"to_user": to_user, "content": content}) as resp:
                return resp.status == 200
        except Exception as e:
            logger.error(f"发送消息失败: {e}")
            return False

    async def send_typing(self, to_user: str) -> bool:
        try:
            session = await self._get_session()
            async with session.post(f"{self.api_base}/api/typing", json={"to_user": to_user}) as resp:
                return resp.status == 200
        except Exception as e:
            logger.error(f"发送 typing 失败: {e}")
            return False