from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import Response
from pydantic import BaseModel, Field

from app.memory import extract_memory_candidates
from app.models import ChatRequest, ChatResponse
from app.openai_client import brain
from app.safety import evaluate_safety
from app.config import settings
from app.tts import synthesize_sweet_voice


class TtsRequest(BaseModel):
    text: str = Field(min_length=1, max_length=1200)
    voice: str = ""


app = FastAPI(title="AnaAI Private Companion API", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "ai-girl-companion"}


@app.get("/")
def root() -> dict[str, str]:
    return health()


@app.post("/v1/chat", response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    safety = evaluate_safety(
        request.user_message,
        adult_confirmed=request.profile.adult_confirmed,
    )
    if not safety.allowed:
        return ChatResponse(
            reply=safety.reply,
            emotion=safety.emotion,
            voice_tone="serious" if safety.emotion == "angry" else "calm",
            safety_level=safety.level,
            freeze_seconds=safety.freeze_seconds,
            memory_candidates=[],
        )

    response = brain.generate(request)
    local_memories = extract_memory_candidates(request.user_message)
    response.memory_candidates = (response.memory_candidates + local_memories)[:3]
    return response


@app.post("/v1/tts")
async def tts(request: TtsRequest) -> Response:
    try:
        audio = await synthesize_sweet_voice(
            request.text,
            request.voice.strip() or settings.tts_voice,
        )
    except Exception as exc:
        raise HTTPException(status_code=502, detail="Voice generation failed") from exc

    return Response(
        content=audio,
        media_type="audio/mpeg",
        headers={"Cache-Control": "no-store"},
    )
