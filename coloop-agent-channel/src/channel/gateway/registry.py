"""平台注册表

开发者: colin.cheng.ai
"""

import logging
from typing import Dict, Optional
from .platform import Platform

logger = logging.getLogger(__name__)


class PlatformRegistry:
    def __init__(self):
        self._platforms: Dict[str, Platform] = {}

    def register(self, platform: Platform) -> None:
        name = platform.get_name()
        self._platforms[name] = platform

    def get(self, name: str) -> Optional[Platform]:
        return self._platforms.get(name)

    async def connect_all(self) -> None:
        for name, platform in self._platforms.items():
            try:
                await platform.connect()
            except Exception as e:
                logger.error(f"平台 {name} 连接失败: {e}")

    async def disconnect_all(self) -> None:
        for name, platform in self._platforms.items():
            try:
                await platform.disconnect()
            except Exception as e:
                logger.error(f"平台 {name} 断开失败: {e}")
