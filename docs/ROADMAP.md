# Phase By Phase Development Roadmap

## Phase 1: MVP Brain And Chat

Goal: make Ana feel emotionally consistent through text.

Deliverables:

- Android app shell
- 18+ onboarding
- Guest profile
- WhatsApp-style chat
- Ana personality engine
- Relationship style selector
- Safety layer
- Local memory
- FastAPI backend
- OpenAI integration through backend only

Status: scaffolded in this repository.

## Phase 2: Basic Voice

Goal: make Ana talk and listen with near-free tools first.

Deliverables:

- Android SpeechRecognizer input
- Android device TextToSpeech output
- Voice screen
- Stop speaking button
- Voice uses the same Ana personality and memory

Status: scaffolded in Android app.

## Phase 3: Memory Improvements

Goal: make Ana remember important talks without becoming creepy.

Deliverables:

- Better memory extraction
- Memory importance scoring
- User can edit memories
- Delete individual memory
- Delete all local data
- "Forget this" command

Future upgrade:

- Supabase PostgreSQL
- pgvector semantic memory search
- Encrypted cloud sync

## Phase 4: Greetings And Scheduled Wishes

Goal: make Ana feel present in daily life.

Deliverables:

- Good morning notification
- Good afternoon notification
- Good night notification
- Birthday wish
- User-controlled notification timing

Recommended Android tools:

- WorkManager for local schedules
- Firebase Cloud Messaging later for server-driven messages

## Phase 5: Anime Avatar MVP

Goal: give Ana a visible identity before full 3D.

Deliverables:

- Static anime avatar
- Emotion labels from backend
- Emotion-based UI styling

Future upgrade:

- Expression variants: happy, sad, loving, angry, playful, calm
- Lightweight animation

## Phase 6: Unity Full 3D Body

Goal: turn Ana into a full 3D character.

Deliverables:

- Unity module embedded in Android
- 3D adult female avatar
- Idle animation
- Talking animation
- Facial blendshapes
- Lip sync
- Emotion animation map

Recommended tools:

- Unity
- Blender
- Ready Player Me / VRoid / Character Creator
- Mixamo
- SALSA LipSync or Oculus LipSync

## Phase 7: Production Backend

Goal: move from local guest MVP to scalable service.

Deliverables:

- Firebase Auth
- Supabase PostgreSQL
- pgvector memory search
- Render hosting
- User profiles
- Rate limits
- API key protection
- Logs without sensitive message storage by default

## Phase 8: Launch Readiness

Goal: prepare for Play Store.

Deliverables:

- Privacy policy
- Terms of service
- Data safety form
- Age gate
- Content policy
- Data deletion
- Abuse reporting
- Crash reporting
- Store screenshots
- Subscription/payment system

