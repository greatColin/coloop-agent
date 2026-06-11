"""CLI 入口"""

import argparse
import asyncio
import logging
import signal

import yaml

from ..agent.test import TestStrategy
from ..gateway.consumer import StreamConsumer
from ..gateway.dispatch import StreamDispatch
from ..gateway.registry import PlatformRegistry
from ..platform.wechat.wechat import WeChatPlatform


class ChannelApp:
    def __init__(self, agent=None):
        self.agent = agent or TestStrategy()
        self.registry = PlatformRegistry()
        self.consumer = StreamConsumer()
        self.dispatch = StreamDispatch()

    async def start(self, config: dict) -> None:
        wechat_config = config.get("wechat", {})
        if wechat_config.get("enabled"):
            platform = WeChatPlatform(
                token=wechat_config["token"],
                api_base=wechat_config.get("api_base", "https://ilinkai.weixin.qq.com"),
            )
            platform.set_message_handler(self.consumer.on_message)
            self.registry.register(platform)

        await self.consumer.start()
        await self.dispatch.start(self.agent, self.registry, self.consumer)
        await self.registry.connect_all()

        logging.info("应用已启动")
        stop = asyncio.Event()
        loop = asyncio.get_event_loop()
        for sig in (signal.SIGINT, signal.SIGTERM):
            loop.add_signal_handler(sig, stop.set)
        await stop.wait()

    async def stop(self) -> None:
        await self.registry.disconnect_all()
        await self.dispatch.stop()
        await self.consumer.stop()


def main():
    parser = argparse.ArgumentParser(description="Channel - AI Agent 远程控制")
    parser.add_argument("-c", "--config", default="config.yaml")
    parser.add_argument("-v", "--verbose", action="store_true")
    args = parser.parse_args()

    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(asctime)s %(name)s %(levelname)s %(message)s",
    )

    try:
        with open(args.config) as f:
            config = yaml.safe_load(f)
    except FileNotFoundError:
        config = {}

    app = ChannelApp()
    try:
        asyncio.run(app.start(config))
    except KeyboardInterrupt:
        asyncio.run(app.stop())


if __name__ == "__main__":
    main()
