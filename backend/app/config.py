import os
from dataclasses import dataclass

from pathlib import Path

from dotenv import load_dotenv

# Ensure backend/.env is found regardless of current working directory
_env_path = Path(__file__).resolve().parent.parent / ".env"
if _env_path.exists():
    load_dotenv(_env_path)
load_dotenv()


@dataclass(frozen=True)
class Settings:
    app_env: str = os.getenv("APP_ENV", "development")
    openai_api_key: str = os.getenv("OPENAI_API_KEY", "")
    openai_model: str = os.getenv("OPENAI_MODEL", "gpt-4.1-mini")
    openai_base_url: str = os.getenv("OPENAI_BASE_URL", "")
    app_site_url: str = os.getenv("APP_SITE_URL", "http://localhost")
    app_title: str = os.getenv("APP_TITLE", "AnaAI")
    tts_voice: str = os.getenv("TTS_VOICE", "hi-IN-SwaraNeural")

    @property
    def is_openrouter(self) -> bool:
        return self.openai_base_url.startswith("https://openrouter.ai") or self.openai_api_key.startswith("sk-or-")

    @property
    def is_river(self) -> bool:
        return self.openai_base_url.startswith("https://api.river.ai") or self.openai_api_key.startswith("rv_")

    @property
    def resolved_base_url(self) -> str:
        if self.openai_base_url:
            return self.openai_base_url
        if self.openai_api_key.startswith("sk-or-"):
            return "https://openrouter.ai/api/v1"
        if self.openai_api_key.startswith("rv_"):
            return "https://api.river.ai/v1"
        return ""

    @property
    def resolved_model(self) -> str:
        model = self.openai_model.strip()
        if self.is_openrouter and model.upper() == "GPT-OSS-120B":
            return "openai/gpt-oss-120b"
        if self.is_river and (not model or model == "gpt-4.1-mini"):
            return "deepseek-ai/DeepSeek-V4.1-Flash-1017"
        return model


settings = Settings()
