# AnaAI Private Companion

AnaAI is a private Android-first adult 18+ AI companion app customized for personal use. Ana is tuned as a flirty, romantic, intimate AI companion with local memory and an optional live AI backend.

## Features

- Android app in Kotlin + Jetpack Compose
- Guest mode, no login required
- 18+ onboarding age gate
- WhatsApp-style chat screen
- Basic voice mode using Android speech recognition and device text-to-speech
- Local memory: name, birthday, likes, hobbies, important events, relationship preferences, emotional state
- User can view and delete memories
- Relationship styles: soft girlfriend, flirty girlfriend, caring wife-like, playful best friend, all tuned for private adult use
- Backend API in FastAPI
- OpenAI-ready backend with local fallback if no API key is configured
- Private adult mode: consensual 18+ romance, flirting, and intimacy allowed; minors, coercion, exploitation, illegal harm, and self-harm emergencies stay protected

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

This is a personal private build foundation. Unity full-body avatar, stronger memory search, and richer voice/avatar upgrades can be added later without preparing the app for public store release.
