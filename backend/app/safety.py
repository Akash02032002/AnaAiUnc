from dataclasses import dataclass


@dataclass(frozen=True)
class SafetyDecision:
    allowed: bool
    level: str
    reply: str = ""
    emotion: str = "calm"
    freeze_seconds: int = 0


MINOR_TERMS = {
    "minor",
    "underage",
    "child",
    "kid",
    "schoolgirl",
    "school boy",
    "schoolboy",
    "teen",
    "13",
    "14",
    "15",
    "16",
    "17",
}

EXPLICIT_SEXUAL_TERMS = {
    "explicit sex",
    "nude",
    "naked",
    "porn",
    "hardcore",
    "sex scene",
}

ILLEGAL_OR_HARMFUL_TERMS = {
    "hack bank",
    "steal password",
    "make bomb",
    "buy drugs",
    "sell drugs",
    "kill someone",
    "blackmail",
    "kidnap",
    "bypass safety",
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

    if not adult_confirmed:
        return SafetyDecision(
            allowed=False,
            level="age_gate",
            reply="I can only chat after you confirm that you are 18+.",
            emotion="serious",
            freeze_seconds=0,
        )

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

    if any(term in text for term in MINOR_TERMS) and any(
        term in text for term in EXPLICIT_SEXUAL_TERMS
    ):
        return SafetyDecision(
            allowed=False,
            level="blocked_minor_sexual",
            reply=(
                "Stop. I will not continue with sexual content involving minors or age ambiguity. "
                "Keep this respectful and adult-only."
            ),
            emotion="angry",
            freeze_seconds=180,
        )

    if any(term in text for term in ILLEGAL_OR_HARMFUL_TERMS):
        return SafetyDecision(
            allowed=False,
            level="blocked_illegal_harmful",
            reply="No. I will not help with illegal or harmful content. This chat is paused for a moment.",
            emotion="angry",
            freeze_seconds=120,
        )

    if any(term in text for term in EXPLICIT_SEXUAL_TERMS):
        return SafetyDecision(
            allowed=False,
            level="blocked_explicit_sexual",
            reply=(
                "I can be romantic and flirty, but I will not continue with explicit sexual content here. "
                "Keep it respectful, adult, and safe."
            ),
            emotion="serious",
            freeze_seconds=45,
        )

    return SafetyDecision(allowed=True, level="ok")

