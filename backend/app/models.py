from pydantic import BaseModel, Field


class UserProfile(BaseModel):
    name: str = "Friend"
    birthday: str = ""
    relationship_style: str = "soft_girlfriend"
    language_style: str = "Hinglish + English"
    adult_confirmed: bool = False


class ChatMessage(BaseModel):
    role: str
    content: str


class MemoryItem(BaseModel):
    id: str = ""
    type: str = "note"
    content: str
    importance: float = 0.5


class ChatRequest(BaseModel):
    user_message: str = Field(min_length=1, max_length=3000)
    profile: UserProfile = Field(default_factory=UserProfile)
    recent_messages: list[ChatMessage] = Field(default_factory=list)
    memories: list[MemoryItem] = Field(default_factory=list)


class MemoryCandidate(BaseModel):
    type: str
    content: str
    importance: float = 0.5


class ChatResponse(BaseModel):
    reply: str
    emotion: str = "calm"
    voice_tone: str = "warm"
    safety_level: str = "ok"
    freeze_seconds: int = 0
    memory_candidates: list[MemoryCandidate] = Field(default_factory=list)

