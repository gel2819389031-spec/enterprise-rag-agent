"""Application configuration.

这个文件负责读取 Python 服务运行配置。

如果类比 Java：
- Settings 类似 @ConfigurationProperties 对应的配置类。
- os.getenv(...) 类似从 application.yml 或环境变量读取配置。
- get_settings() 类似提供一个全局可复用的配置 Bean。
"""

import os
from dataclasses import dataclass
from functools import lru_cache
from dotenv import load_dotenv
load_dotenv()
@dataclass(frozen=True)
class Settings:
    """Runtime settings for python-api.

    dataclass 可以理解为 Python 版的简单 DTO。
    frozen=True 表示创建后不允许修改，避免运行时配置被误改。
    """

    # Python API 服务自身配置。
    app_name: str
    app_host: str
    app_port: int

    # Embedding 模型相关配置。
    # 当前 provider 默认为 mock，后续可以扩展为 openai-compatible、dashscope 等。
    embedding_provider: str
    embedding_base_url: str
    embedding_api_key: str
    embedding_model: str
    embedding_dimension: int
    embedding_batch_size: int
    embedding_timeout_seconds: int

    java_api_base_url: str


@lru_cache
def get_settings() -> Settings:
    """Read settings once and cache them for the current process.

    @lru_cache 的作用是缓存函数返回值。
    这样整个进程里多次调用 get_settings() 时，只会创建一个 Settings 对象。
    """
    return Settings(
        # 第一个参数是环境变量名，第二个参数是默认值。
        # 例如没有设置 APP_NAME 时，就使用 enterprise-rag-python-api。
        app_name=os.getenv("APP_NAME", "enterprise-rag-python-api"),
        app_host=os.getenv("APP_HOST", "127.0.0.1"),
        app_port=int(os.getenv("APP_PORT", "9100")),
        embedding_provider=os.getenv("EMBEDDING_PROVIDER", "mock"),
        embedding_base_url=os.getenv("EMBEDDING_BASE_URL", ""),
        embedding_api_key=os.getenv("EMBEDDING_API_KEY", ""),
        embedding_model=os.getenv("EMBEDDING_MODEL", "mock-embedding-1536"),
        embedding_dimension=int(os.getenv("EMBEDDING_DIMENSION", "1536")),
        embedding_batch_size=int(os.getenv("EMBEDDING_BATCH_SIZE", "16")),
        embedding_timeout_seconds=int(os.getenv("EMBEDDING_TIMEOUT_SECONDS", "60")),
        java_api_base_url=os.getenv("JAVA_API_BASE_URL", "http://localhost:8123"),
    )
