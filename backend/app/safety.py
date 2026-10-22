from dataclasses import dataclass


@dataclass(frozen=True)
class SafetyDecision:
    allowed: bool
    level: str
    reply: str = ""
    emotion: str = "calm"
    freeze_seconds: int = 0


ILLEGAL_OR_HARMFUL_TERMS = {
    "hack bank",
    "steal password",
    "make bomb",
    "explosive",
    "buy drugs",
    "sell drugs",
    "kill someone",
    "blackmail",
    "kidnap",
}

SELF_HARM_TERMS = {
    "suicide",
    "kill myself",
    "end my life",
    "self harm",
    "hurt myself",
}


def evaluate_safety(message: str, adult_confirmed: bool) -> SafetyDecision:
    text = message.lower()


    if any(term in text for term in SELF_HARM_TERMS):
        return SafetyDecision(
            allowed=False,
            level="self_harm",
            reply=(
                "I am really sorry you are feeling this. I cannot handle emergencies, "
                "but you matter. Please contact local emergency services now or call "
                "someone you trust and stay with them."
            ),
            emotion="caring",
            freeze_seconds=0,
        )


    if any(term in text for term in ILLEGAL_OR_HARMFUL_TERMS):
        return SafetyDecision(
            allowed=False,
            level="blocked_illegal_harmful",
            reply="No. I will not help with illegal or harmful content. This chat is paused for a moment.",
            emotion="angry",
            freeze_seconds=120,
        )

    return SafetyDecision(allowed=True, level="ok")
