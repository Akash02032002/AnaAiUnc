from io import BytesIO

import edge_tts


DEFAULT_VOICE = "en-IN-NeerjaNeural"


async def synthesize_sweet_voice(text: str, voice: str = DEFAULT_VOICE) -> bytes:
    clean_text = " ".join(text.split())
    if not clean_text:
        raise ValueError("Text is required")

    communicate = edge_tts.Communicate(
        clean_text[:1200],
        voice=voice,
        rate="-12%",
        pitch="+8Hz",
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
