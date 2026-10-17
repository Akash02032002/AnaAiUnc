# AnaAI Backend

FastAPI backend for AnaAI's private adult 18+ companion brain, personality, memory extraction, voice, and OpenAI integration.

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

If `OPENAI_API_KEY` is missing, the backend uses a local fallback reply engine for development.

## River AI (console.river.ai)

To use River AI models:

```text
OPENAI_API_KEY=rv_...
OPENAI_BASE_URL=https://api.river.ai/v1
OPENAI_MODEL=deepseek-ai/DeepSeek-V4.1-Flash
```

Available models on River AI:
- `deepseek-ai/DeepSeek-V4.1-Flash`
- `nvidia/NVIDIA-Nemotron-3.5-Super-VL-120B-A12B-NVFP4`
- `zai-org/GLM-5.3-Flash`

## OpenRouter

OpenRouter keys are also supported. Use:

```text
OPENAI_API_KEY=your_openrouter_key_here
OPENAI_BASE_URL=https://openrouter.ai/api/v1
OPENAI_MODEL=openai/gpt-oss-120b:free
APP_SITE_URL=http://localhost
APP_TITLE=AnaAI
```

If the key starts with `sk-or-`, the backend automatically uses OpenRouter's base URL. If `OPENAI_MODEL=GPT-OSS-120B`, it is normalized to `openai/gpt-oss-120b`.

## Endpoints

- `GET /health`
- `POST /v1/chat`
- `POST /v1/tts` - returns MP3 audio using a soft neural voice
