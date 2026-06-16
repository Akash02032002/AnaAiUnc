# AI Girl Companion Backend

FastAPI backend for Ana's MVP brain, safety, personality, and OpenAI integration.

## Setup

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## Environment

Set these values in `.env`:

```text
OPENAI_API_KEY=your_key_here
OPENAI_MODEL=openai/gpt-oss-120b:free
```

If `OPENAI_API_KEY` is missing, the backend uses a safe local fallback reply engine for development.

## OpenRouter

OpenRouter keys are also supported. Use:

```text
OPENAI_API_KEY=your_openrouter_key_here
OPENAI_BASE_URL=https://openrouter.ai/api/v1
OPENAI_MODEL=openai/gpt-oss-120b:free
APP_SITE_URL=http://localhost
APP_TITLE=AI Girl Companion
```

If the key starts with `sk-or-`, the backend automatically uses OpenRouter's base URL. If `OPENAI_MODEL=GPT-OSS-120B`, it is normalized to `openai/gpt-oss-120b`.

## Endpoints

- `GET /health`
- `POST /v1/chat`
- `POST /v1/tts` - returns MP3 audio using a soft neural voice
