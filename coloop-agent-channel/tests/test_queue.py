import pytest
import asyncio
from channel.core.message import Message
from channel.core.queue import MessageQueue


@pytest.mark.asyncio
async def test_put_and_get():
    queue = MessageQueue()
    msg = Message(msg_id="msg_001", platform="wechat", user_id="u1", chat_id="c1", content="Hello")
    await queue.put(msg)
    assert not queue.empty
    result = await queue.get()
    assert result.msg_id == "msg_001"
    assert queue.empty


@pytest.mark.asyncio
async def test_get_if_any_with_message():
    queue = MessageQueue()
    msg = Message(msg_id="msg_002", platform="wechat", user_id="u1", chat_id="c1", content="Test")
    await queue.put(msg)
    result = await queue.get_if_any()
    assert result is not None
    assert result.msg_id == "msg_002"


@pytest.mark.asyncio
async def test_get_if_any_empty():
    queue = MessageQueue()
    result = await queue.get_if_any()
    assert result is None


@pytest.mark.asyncio
async def test_fifo_order():
    queue = MessageQueue()
    for i in range(3):
        await queue.put(Message(msg_id=f"msg_{i:03d}", platform="wechat", user_id="u1", chat_id="c1", content=f"M{i}"))
    for i in range(3):
        result = await queue.get()
        assert result.msg_id == f"msg_{i:03d}"
