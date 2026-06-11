"""消息模型

@developer colin.cheng.ai
"""

from dataclasses import dataclass, field
from typing import Optional, Dict, Any
import time


@dataclass
class Message:
    """统一消息模型，跨平台使用的消息格式"""
    msg_id: str
    platform: str
    user_id: str
    chat_id: str
    content: str
    msg_type: str = "text"
    media_url: Optional[str] = None
    media_key: Optional[str] = None
    timestamp: float = field(default_factory=time.time)
    raw: Optional[Dict[str, Any]] = None
