import os
from dataclasses import dataclass

from dotenv import load_dotenv


load_dotenv()


@dataclass(frozen=True)
class Settings:
    app_env: str = os.getenv("APP_ENV", "development")
    openai_api_key: str = os.getenv("OPENAI_API_KEY", "")
    openai_model: str = os.getenv("OPENAI_MODEL", "gpt-4.1-mini")
    openai_base_url: str = os.getenv("OPENAI_BASE_URL", "")
    app_site_url: str = os.getenv("APP_SITE_URL", "http://localhost")
    app_title: str = os.getenv("APP_TITLE", "AnaAI")
    tts_voice: str = os.getenv("TTS_VOICE", "en-IN-NeerjaNeural")

    @property
    def is_openrouter(self) -> bool:
        return self.openai_base_url.startswith("https://openrouter.ai") or self.openai_api_key.startswith("sk-or-")

    @property
    def resolved_base_url(self) -> str:
        if self.openai_base_url:
            return self.openai_base_url
        if self.openai_api_key.startswith("sk-or-"):
            return "https://openrouter.ai/api/v1"
        return ""

    @property
    def resolved_model(self) -> str:
        model = self.openai_model.strip()
        if self.is_openrouter and model.upper() == "GPT-OSS-120B":
            return "openai/gpt-oss-120b"
        return model


settings = Settings()
