import re

from app.models import MemoryCandidate


PATTERNS: list[tuple[str, str, float]] = [
    (r"\bmy name is ([a-zA-Z ]{2,40})", "name", 0.95),
    (r"\bcall me ([a-zA-Z ]{2,40})", "name", 0.85),
    (r"\bmy birthday is ([a-zA-Z0-9 ,/-]{3,40})", "birthday", 0.95),
    (r"\bi like ([a-zA-Z0-9 ,.'-]{2,80})", "preference", 0.65),
    (r"\bi love ([a-zA-Z0-9 ,.'-]{2,80})", "preference", 0.75),
    (r"\bi hate ([a-zA-Z0-9 ,.'-]{2,80})", "dislike", 0.7),
    (r"\bmy favorite ([a-zA-Z ]{2,30}) is ([a-zA-Z0-9 ,.'-]{2,80})", "favorite", 0.75),
]


def extract_memory_candidates(message: str) -> list[MemoryCandidate]:
    memories: list[MemoryCandidate] = []
    lower = message.lower()

    for pattern, memory_type, importance in PATTERNS:
        match = re.search(pattern, lower)
        if not match:
            continue

        if memory_type == "favorite" and len(match.groups()) >= 2:
            content = f"User's favorite {match.group(1).strip()} is {match.group(2).strip()}."
        else:
            content = match.group(0).strip().capitalize()
            if not content.endswith("."):
                content += "."

        memories.append(
            MemoryCandidate(type=memory_type, content=content, importance=importance)
        )

    return memories[:3]

