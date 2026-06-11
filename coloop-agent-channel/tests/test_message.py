import pytest
from channel.core.message import Message


def test_create_message():
    """测试创建消息"""
    msg = Message(
        msg_id="msg_001",
        platform="wechat",
        user_id="user_123",
        chat_id="chat_456",
        content="Hello"
    )
    assert msg.msg_id == "msg_001"
    assert msg.platform == "wechat"
    assert msg.user_id == "user_123"
    assert msg.chat_id == "chat_456"
    assert msg.content == "Hello"
    assert msg.msg_type == "text"


def test_message_defaults():
    """测试消息默认值"""
    msg = Message(
        msg_id="msg_002",
        platform="wechat",
        user_id="user_123",
        chat_id="chat_456",
        content="Test"
    )
    assert msg.msg_type == "text"
    assert msg.media_url is None
    assert msg.media_key is None
    assert msg.raw is None
    assert msg.timestamp > 0


def test_message_with_media():
    """测试带媒体的消息"""
    msg = Message(
        msg_id="msg_003",
        platform="wechat",
        user_id="user_123",
        chat_id="chat_456",
        content="",
        msg_type="image",
        media_url="https://example.com/image.jpg",
        media_key="aes_key_123"
    )
    assert msg.msg_type == "image"
    assert msg.media_url == "https://example.com/image.jpg"
    assert msg.media_key == "aes_key_123"
