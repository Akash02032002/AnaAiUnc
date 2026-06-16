# AI Girl Companion MVP

AI Girl Companion is an Android-first MVP for an adult 18+ companion named Ana. This scaffold starts with the smartest path: build the brain and personality first, then upgrade the avatar/body later.

## MVP Features

- Android app in Kotlin + Jetpack Compose
- Guest mode, no login required
- 18+ onboarding age gate
- WhatsApp-style chat screen
- Basic voice mode using Android speech recognition and device text-to-speech
- Local memory: name, birthday, likes, hobbies, important events, relationship preferences, emotional state
- User can view and delete memories
- Relationship styles: soft girlfriend, flirty girlfriend, caring wife-like, playful best friend
- Backend API in FastAPI
- OpenAI-ready backend with safe fallback if no API key is configured
- Adult safety rules: romance/flirting allowed, explicit sexual content blocked in MVP

## Project Structure

```text
android/       Android app
backend/       FastAPI backend for Ana brain/personality/safety
docs/          roadmap, legal, and safety documents
```

## Quick Start

### Backend

```bash
cd backend
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Set `OPENAI_API_KEY` in `backend/.env` when you are ready to use live AI. Without it, the backend returns safe demo replies.

### Android

Open `android/` in Android Studio and run the `app` configuration.

For the connected physical Android device, the default backend URL is:

```text
http://127.0.0.1:8000
```

Use `adb reverse tcp:8000 tcp:8000` before launching. For an emulator, change the backend URL in Settings to:

```text
http://10.0.2.2:8000
```

For a physical Android phone without USB reverse, change the backend URL in Settings to your PC LAN IP, for example:

```text
http://192.168.1.10:8000
```

## Current Scope

This is a real MVP foundation, not the final 3D version. Unity full-body avatar, Firebase Auth, subscriptions, cloud memory, and pgvector/Supabase are planned as later phases.
