# Voice Input/Output Pipeline

**Status:** TBD — highest-priority feature after baseline. Voice + handoff card alone can win.

## What this file will contain

- ASR (speech-to-text) integration: Android SpeechRecognizer vs Whisper.cpp small vs Gemma 4 native audio
- TTS (text-to-speech) integration: Android built-in TextToSpeech, voice/locale config
- Hindi support: Gemma 4 small models support native audio multilingually — confirm on E2B
- "Panic-friendly" UX: large mic button, hands-free flow, no taps required after first start
- Pipeline: voice → ASR text → Gemma triage → response → TTS → speak

## Decisions still open

- ASR runtime:
  - **Android built-in:** free, fast, but quality varies by language and OS version
  - **Whisper.cpp small:** consistent quality, larger binary
  - **Gemma 4 native audio:** elegant but unproven on E2B — test early
  - **Default:** Android built-in for v1; switch if Hindi accuracy is bad

## Hindi-first considerations

- Gemma 4 E2B Hindi audio quality: validate on day 3
- TTS Hindi voice: Android `Locale("hi", "IN")` — test naturalness
- If Android Hindi TTS sounds robotic, fall back to English-only for v1, mark Hindi as stretch

## Pass criteria for "done"

- "Someone is bleeding from the leg, what do I do?" → spoken response in <8 seconds
- Hindi version of same input works (or English-only confirmed acceptable for v1)
- Works in airplane mode
- Mic button single-press starts; auto-stops on silence
