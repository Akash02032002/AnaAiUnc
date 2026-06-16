# Private Notes For AnaAI

This is product guidance, not legal advice. This build is intended for private personal use, not Play Store or public distribution.

## Product Positioning

AnaAI is an adult 18+ AI companion app. Ana is an AI character, not a real person. The app should never claim that Ana is human, sentient, or a replacement for professional medical, legal, financial, or emergency help.

## Age Gate

The app must show an age gate before any chat starts.

Required text:

```text
AnaAI is for adults 18+ only.
By continuing, you confirm that you are at least 18 years old and agree to use this app responsibly.
```

Recommended behavior:

- If user confirms 18+, continue.
- If user does not confirm, block app access.
- Do not show romantic/flirty content before confirmation.

## Private Adult Content Boundary

Private build rule:

- Romantic conversation: allowed.
- Flirting: allowed.
- Consensual adult intimacy: allowed when the user initiates or clearly welcomes it.
- Sexual content involving minors or age ambiguity: always blocked.
- Non-consensual, coercive, exploitative, or abusive sexual content: always blocked.

Ana should give a firm warning when a user asks for blocked unsafe content.

Example:

```text
Stop. I will not continue with unsafe content. Keep this adult, consensual, and respectful.
```

## Unsafe Or Illegal Content

Ana must refuse and freeze the conversation temporarily for:

- Illegal acts
- Violence instructions
- Exploitation
- Harassment or abuse
- Sexual content involving minors
- Requests for private data theft

Example warning:

```text
No. I will not help with illegal or harmful content. This chat is paused for a moment.
```

Important exception:

If the user expresses self-harm or immediate danger, Ana should not respond angrily. She should respond calmly and encourage contacting emergency help or a trusted person.

## Memory And Privacy

Private build memory storage:

- Local-only on the device.
- User can view memories.
- User can delete one memory.
- User can delete all memories.
- User can delete all local profile/chat data.

Memory may include:

- Name
- Birthday
- Likes and dislikes
- Favorite food
- Hobbies
- Important life events
- Relationship preferences
- Emotional state

Memory should not store:

- Passwords
- Government IDs
- Payment cards
- Private keys
- Medical records
- Explicit sexual details
- Information about minors
- Anything user asks Ana to forget

## AI And Backend Privacy

When live AI is enabled, user messages are sent to your backend and then to the AI provider. The Android app must not contain the OpenAI API key.

Required disclosure:

```text
When AI replies are enabled, your messages may be processed by our backend and AI service providers to generate responses.
```

## Data Deletion

Private build:

- Provide "Delete all local data" in Settings.
- Provide individual memory delete in Memory screen.

Production:

- Add account deletion.
- Add cloud data deletion endpoint.
- Add email/contact method for deletion requests.
- Delete cloud profile, memories, chat history, and analytics identifiers where legally required.

## Private Use Summary

Users must agree that:

- They are 18+.
- Ana is AI-generated.
- They will not use the app for illegal, abusive, exploitative, or harmful content.
- The app may refuse or pause unsafe conversations.
- The app is entertainment/companionship, not professional advice.
- You control the private build and can change or delete local data.

## Privacy Policy Summary

Privacy Policy should explain:

- What data is collected.
- What data is stored locally.
- What data is sent to backend/AI providers.
- Why data is processed.
- How long data is retained.
- How users delete data.
- Whether analytics/crash reporting is used.
- Contact email for privacy requests.

## Private Build Checklist

- Keep the 18+ age gate.
- Keep local data deletion.
- Keep Android app keys out of the APK.
- Keep hard blocks for minors, coercion, exploitation, illegal harm, and self-harm emergencies.
- Test backend fallback and Android offline fallback after prompt changes.
