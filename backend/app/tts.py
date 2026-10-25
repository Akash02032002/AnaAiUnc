from io import BytesIO
import unicodedata

import edge_tts


DEFAULT_VOICE = "hi-IN-SwaraNeural"


def clean_text_for_speech(text: str) -> str:
    result = []
    for ch in text:
        cat = unicodedata.category(ch)
        code = ord(ch)
        if cat in ("So", "Cs", "Sk") or 0x1F000 <= code <= 0x1FFFF or 0xFE00 <= code <= 0xFE0F or code in (0x200D, 0x20E3):
            continue
        if 0x2600 <= code <= 0x27BF:
            continue
        if ch in ("*", "_", "~", "`"):
            continue
        result.append(ch)
    cleaned = "".join(result)
    cleaned = " ".join(cleaned.split()).strip()
    return cleaned if cleaned else text


async def synthesize_sweet_voice(text: str, voice: str = "") -> bytes:
    clean_text = clean_text_for_speech(text)
    if not clean_text:
        raise ValueError("Text is required")

    selected_voice = voice.strip()
    if not selected_voice or selected_voice == "en-IN-NeerjaNeural":
        selected_voice = DEFAULT_VOICE

    communicate = edge_tts.Communicate(
        clean_text[:1200],
        voice=selected_voice,
        rate="-7%",
        pitch="+5Hz",
        volume="+0%",
    )
    audio = BytesIO()
    async for chunk in communicate.stream():
        if chunk["type"] == "audio":
            audio.write(chunk["data"])

    data = audio.getvalue()
    if not data:
        raise RuntimeError("No audio returned from TTS provider")
    return data
