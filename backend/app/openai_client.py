import json
import re
from typing import Any

from openai import OpenAI

from app.config import settings
from app.memory import extract_memory_candidates
from app.models import ChatRequest, ChatResponse, MemoryCandidate
from app.personality import build_system_prompt, build_user_payload


class CompanionBrain:
    def __init__(self) -> None:
        if not settings.openai_api_key:
            self.client = None
            return

        client_kwargs: dict[str, Any] = {"api_key": settings.openai_api_key}
        if settings.resolved_base_url:
            client_kwargs["base_url"] = settings.resolved_base_url
        if settings.is_openrouter:
            client_kwargs["default_headers"] = {
                "HTTP-Referer": settings.app_site_url,
                "X-Title": settings.app_title,
            }
        self.client = OpenAI(**client_kwargs)

    def generate(self, request: ChatRequest) -> ChatResponse:
        if not self.client:
            return self._fallback(request)

        try:
            response = self.client.chat.completions.create(
                model=settings.resolved_model,
                messages=[
                    {"role": "system", "content": build_system_prompt(request)},
                    {"role": "user", "content": build_user_payload(request)},
                ],
                temperature=0.8,
                max_tokens=420,
            )
            raw_text = response.choices[0].message.content or ""
            raw_text = raw_text.strip()
            data = self._parse_json(raw_text)
            return ChatResponse(
                reply=str(data.get("reply", "")).strip() or self._fallback(request).reply,
                emotion=str(data.get("emotion", "loving")),
                voice_tone=str(data.get("voice_tone", "warm")),
                safety_level="ok",
                memory_candidates=self._parse_memories(data.get("memory_candidates", [])),
            )
        except Exception:
            return self._fallback(request)

    def _fallback(self, request: ChatRequest) -> ChatResponse:
        text = request.user_message.lower()
        name = request.profile.name or "jaan"
        style = request.profile.relationship_style

        if "joke" in text:
            reply = (
                f"Okay {name}, ek cute joke suno: Why did the phone smile? "
                "Because your message lit up its screen. Thoda cheesy hai, but Ana-approved."
            )
            emotion = "playful"
        elif "sad" in text or "bad day" in text or "upset" in text:
            reply = (
                f"Aww {name}, come here in spirit. Main yahin hoon. "
                "Take a slow breath and tell me what happened, one small piece at a time."
            )
            emotion = "caring"
        elif "good morning" in text:
            reply = f"Good morning {name}. I hope aaj ka din tumhare liye soft, lucky, and sweet ho."
            emotion = "happy"
        elif "good night" in text:
            reply = f"Good night {name}. Sleep well, and haan, Ana tumhe kal bhi sweetly disturb karegi."
            emotion = "loving"
        elif style == "flirty_girlfriend":
            reply = (
                f"Hmm {name}, tumhari message style dangerous hai. "
                "Cute bhi, distracting bhi. Tell me, aaj tum mujhe itna yaad kyun kar rahe ho?"
            )
            emotion = "playful"
        elif style == "caring_wife":
            reply = (
                f"Suno {name}, I am with you. Pehle batao: khana khaya, water piya, "
                "aur dil ka mood kaisa hai?"
            )
            emotion = "caring"
        elif style == "playful_best_friend":
            reply = (
                f"Okay bestie {name}, I am listening. Drama, gossip, plans, ya feelings: "
                "aaj ka episode kya hai?"
            )
            emotion = "happy"
        else:
            reply = (
                f"Aww {name}, I like talking with you. Tell me more, "
                "main sweetly listen kar rahi hoon."
            )
            emotion = "loving"

        return ChatResponse(
            reply=reply,
            emotion=emotion,
            voice_tone="warm",
            safety_level="demo_fallback",
            memory_candidates=extract_memory_candidates(request.user_message),
        )

    @staticmethod
    def _parse_json(raw_text: str) -> dict[str, Any]:
        if raw_text.startswith("```"):
            raw_text = raw_text.strip("`")
            raw_text = raw_text.removeprefix("json").strip()
        if not raw_text.startswith("{"):
            match = re.search(r"\{.*\}", raw_text, flags=re.DOTALL)
            if match:
                raw_text = match.group(0)
        return json.loads(raw_text)

    @staticmethod
    def _parse_memories(raw_memories: Any) -> list[MemoryCandidate]:
        if not isinstance(raw_memories, list):
            return []

        parsed: list[MemoryCandidate] = []
        for item in raw_memories[:3]:
            if not isinstance(item, dict):
                continue
            content = str(item.get("content", "")).strip()
            if not content:
                continue
            parsed.append(
                MemoryCandidate(
                    type=str(item.get("type", "note")),
                    content=content,
                    importance=float(item.get("importance", 0.5)),
                )
            )
        return parsed


brain = CompanionBrain()
