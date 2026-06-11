"""平台抽象接口

开发者: colin.cheng.ai
"""

from abc import ABC, abstractmethod
from typing import Callable, Awaitable
from ..core.message import Message


class Platform(ABC):
    """平台抽象接口"""

    @abstractmethod
    async def connect(self) -> None:
        ...

    @abstractmethod
    async def disconnect(self) -> None:
        ...

    @abstractmethod
    async def send_message(self, chat_id: str, content: str) -> bool:
        ...

    @abstractmethod
    async def send_typing(self, chat_id: str) -> bool:
        ...

    @abstractmethod
    def get_name(self) -> str:
        ...

    @abstractmethod
    def is_connected(self) -> bool:
        ...

    def set_message_handler(self, handler: Callable[[Message], Awaitable[None]]) -> None:
        self._handler = handler
