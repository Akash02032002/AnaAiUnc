import base64
import io
import re
from typing import Any

from openai import OpenAI
try:
    import pymupdf as fitz
except ImportError:
    import fitz
from pypdf import PdfReader

from app.config import settings

MODEL_DEEPSEEK = "deepseek-ai/DeepSeek-V4.1-Flash-1017"
MODEL_NVIDIA = "nvidia/NVIDIA-Nemotron-3.5-Super-VL-120B-A12B-NVFP4"


def get_client() -> OpenAI:
    return OpenAI(
        base_url=settings.resolved_base_url or "https://api.river.ai/v1",
        api_key=settings.openai_api_key,
    )


def process_document(file_bytes: bytes, filename: str, user_prompt: str = "") -> str:
    lower_name = filename.lower()
    client = get_client()
    default_question = user_prompt.strip() or "Please read and summarize this document, highlighting the key information."

    # ── CASE 1: Text file ─────────────────────────────────────────────
    if lower_name.endswith((".txt", ".md", ".csv", ".json", ".py", ".log")):
        try:
            text_content = file_bytes.decode("utf-8", errors="ignore").strip()
            if text_content:
                return _call_deepseek(client, text_content, default_question)
        except Exception:
            pass

    # ── Handle PDF file ───────────────────────────────────────────────
    if lower_name.endswith(".pdf"):
        selectable_text = ""
        try:
            reader = PdfReader(io.BytesIO(file_bytes))
            selectable_text = "\n".join(page.extract_text() or "" for page in reader.pages).strip()
        except Exception:
            selectable_text = ""

        # Case 1: Selectable text found (> 50 chars)
        if len(selectable_text) > 50:
            return _call_deepseek(client, selectable_text, default_question)

        # Scanned PDF: Render page to image
        try:
            doc = fitz.open(stream=file_bytes, filetype="pdf")
            if len(doc) > 0:
                page = doc.load_page(0)
                pix = page.get_pixmap(dpi=150)
                img_b64 = base64.b64encode(pix.tobytes("jpeg")).decode("utf-8")
                return _call_nvidia_vision(client, img_b64, default_question)
        except Exception:
            return "Ana can't process this file."

    # ── Direct image file ─────────────────────────────────────────────
    if lower_name.endswith((".png", ".jpg", ".jpeg", ".webp")):
        try:
            img_b64 = base64.b64encode(file_bytes).decode("utf-8")
            return _call_nvidia_vision(client, img_b64, default_question)
        except Exception:
            return "Ana can't process this file."

    # Fallback attempt as text
    try:
        raw = file_bytes.decode("utf-8", errors="ignore").strip()
        if len(raw) > 20:
            return _call_deepseek(client, raw, default_question)
    except Exception:
        pass

    return "Ana can't process this file."


def _call_deepseek(client: OpenAI, text: str, question: str) -> str:
    """Case 1: DeepSeek V4.1 Flash-1017 with full uncensored freedom."""
    try:
        trimmed = text[:40000]  # Large context window
        response = client.chat.completions.create(
            model=MODEL_DEEPSEEK,
            messages=[
                {
                    "role": "system",
                    "content": "You are Ana, a helpful and affectionate companion. Analyze the provided document accurately and respond to the partner's question.",
                },
                {
                    "role": "user",
                    "content": f"Document content:\n\"\"\"\n{trimmed}\n\"\"\"\n\nUser request: {question}",
                },
            ],
            max_tokens=1200,
            temperature=0.7,
        )
        output = response.choices[0].message.content or ""
        return output.strip() or "Ana finished reading, but no content was generated."
    except Exception as e:
        return f"Error analyzing document: {e}"


def _call_nvidia_vision(client: OpenAI, img_b64: str, question: str) -> str:
    """Case 2 & 3: NVIDIA Nemotron 3.5 Super VL for scanned images."""
    try:
        response = client.chat.completions.create(
            model=MODEL_NVIDIA,
            messages=[
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "text",
                            "text": f"Read and extract all information from this scanned document. {question}",
                        },
                        {
                            "type": "image_url",
                            "image_url": {"url": f"data:image/jpeg;base64,{img_b64}"},
                        },
                    ],
                }
            ],
            max_tokens=1000,
            temperature=0.3,
        )
        content = response.choices[0].message.content or ""
        content = content.strip()

        # Case 3: Check for refusal or empty content caused by guardrails
        lower = content.lower()
        if not content or "cannot fulfill" in lower or "safety policy" in lower or "i am unable" in lower:
            return "Ana can't process this file."

        return content
    except Exception:
        # Case 3 refusal / error
        return "Ana can't process this file."
