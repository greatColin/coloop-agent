import pytest
from channel.agent.strategy import AgentStrategy
from channel.agent.test import TestStrategy


def test_agent_strategy_is_abstract():
    with pytest.raises(TypeError):
        AgentStrategy()


@pytest.mark.asyncio
async def test_test_strategy():
    agent = TestStrategy(delay=0.01)
    responses = []
    async for reply in agent.stream("Hello"):
        responses.append(reply)
    assert len(responses) > 0
    assert any("Hello" in r for r in responses)


@pytest.mark.asyncio
async def test_test_strategy_multiple_replies():
    agent = TestStrategy(delay=0.01)
    responses = []
    async for reply in agent.stream("Test"):
        responses.append(reply)
    assert len(responses) >= 3
