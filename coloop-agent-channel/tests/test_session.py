import pytest
import asyncio
from unittest.mock import AsyncMock
from channel.core.message import Message
from channel.core.session import Session
from channel.agent.test import TestStrategy


@pytest.fixture
def mock_sender():
    return AsyncMock()


@pytest.fixture
def test_agent():
    return TestStrategy(delay=0.01)


@pytest.mark.asyncio
async def test_session_process_single_message(mock_sender, test_agent):
    session = Session(chat_id="chat_001", agent=test_agent, send_message=mock_sender)
    msg = Message(msg_id="msg_001", platform="wechat", user_id="u1", chat_id="chat_001", content="Hello")
    await session.on_message(msg)
    assert mock_sender.call_count >= 3


@pytest.mark.asyncio
async def test_session_queue_message_while_processing(mock_sender, test_agent):
    session = Session(chat_id="chat_001", agent=test_agent, send_message=mock_sender)
    session.is_processing = True
    msg = Message(msg_id="msg_002", platform="wechat", user_id="u1", chat_id="chat_001", content="Queued")
    await session.on_message(msg)
    assert not session.queue.empty


@pytest.mark.asyncio
async def test_session_stop(mock_sender, test_agent):
    session = Session(chat_id="chat_001", agent=test_agent, send_message=mock_sender)
    session.is_processing = True
    session.stop()
    assert session._stop is True
