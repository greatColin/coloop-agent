"""集成测试"""

import pytest
import asyncio
from unittest.mock import AsyncMock, MagicMock
from channel.core.message import Message
from channel.core.session import Session
from channel.agent.test import TestStrategy
from channel.gateway.consumer import StreamConsumer
from channel.gateway.dispatch import StreamDispatch
from channel.gateway.registry import PlatformRegistry


@pytest.mark.asyncio
async def test_full_flow():
    agent = TestStrategy(delay=0.01)
    consumer = StreamConsumer()
    dispatch = StreamDispatch()
    registry = PlatformRegistry()

    platform = MagicMock()
    platform.get_name.return_value = "test"
    platform.send_message = AsyncMock(return_value=True)
    platform.is_connected.return_value = True
    registry.register(platform)

    await consumer.start()
    await dispatch.start(agent, registry, consumer)

    msg = Message(msg_id="msg_001", platform="test", user_id="u1", chat_id="c1", content="Hello")
    await consumer.on_message(msg)

    await asyncio.sleep(0.5)
    assert platform.send_message.call_count >= 3

    await dispatch.stop()
    await consumer.stop()


@pytest.mark.asyncio
async def test_message_queuing():
    agent = TestStrategy(delay=0.1)
    send_message = AsyncMock()

    session = Session(chat_id="c1", agent=agent, send_message=send_message)

    msg1 = Message(msg_id="msg_001", platform="test", user_id="u1", chat_id="c1", content="First")
    asyncio.create_task(session.on_message(msg1))
    await asyncio.sleep(0.05)

    msg2 = Message(msg_id="msg_002", platform="test", user_id="u1", chat_id="c1", content="Second")
    await session.on_message(msg2)

    assert not session.queue.empty
