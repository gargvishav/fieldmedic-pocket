# Tech Stack — LOCKED

## Decisions (final)

| Layer | Choice | Reason |
|---|---|---|
| **Phone** | OnePlus 9 (Snapdragon 888, 8–12GB RAM) | Capable Android device, fits Gemma 4 E2B in memory |
| **OS** | Android | Cactus prize + past winners + Google's tooling all live here. iPhone 17 stays as daily phone. |
| **Starter app** | **Fork [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery)** | Already runs Gemma on-device via LiteRT. Apache 2.0. Cuts build time in half. |
| **Model** | Gemma 4 E2B (default), E4B (fallback if E2B fails to hold function schema) | Smallest variant that fits the brief; biggest device coverage |
| **Runtime** | LiteRT-LM (already wired in AI Edge Gallery) | Officially supported Google path, samples work on OnePlus 9 |
| **UI framework** | Jetpack Compose (already in gallery) | No new framework to learn |
| **Storage** | Room (Kotlin entity → SQLite) | Structured handoff card data |
| **TTS (v1)** | Android built-in `TextToSpeech` | Free, on-device, supports Hindi |
| **ASR (v1)** | Android `SpeechRecognizer` | Free, on-device on most phones; switch to Whisper.cpp small if Hindi accuracy is bad |
| **Function calling** | Gemma 4 native function-calling format | Per official docs |
| **Camera** | CameraX | Standard, already in many samples |
| **Build tool** | Android Studio (latest stable) | Required |

## Build philosophy

**Don't build from scratch.** Fork AI Edge Gallery, strip features, add yours on top. Every line of code you don't write is a day saved.

## Why iOS was rejected

- LiteRT-LM does support iOS, but tooling and reference samples are 80% Android-focused
- Cactus prize criteria match Android-first apps better
- Distributing demo to judges via APK is faster than TestFlight
- Iphone 17 ships with Apple Intelligence — the "on-device AI" story is Apple's, not Google's, on iOS

## Risks + mitigations

| Risk | Mitigation |
|---|---|
| E2B inference too slow on OnePlus 9 | Day-3 hard checkpoint. If >10s per query, switch to E2B-quantized variant or smaller prompt. |
| Hindi ASR accuracy poor | Fall back to English-only for v1. Mark Hindi as stretch. |
| AI Edge Gallery has breaking changes | Pin to a specific commit/tag from day 1. Don't update during the sprint. |
| Limited Android experience | Use AI-assisted IDE tooling to scaffold boilerplate. Stick to small commits. |

## Hardware backup plan

If OnePlus 9 fails (model won't load, battery dead, screen breaks):
- Borrow any Android phone with Snapdragon 855+ and 8GB+ RAM
- Worst case: demo on Android Emulator with hardware acceleration (lower polish, but valid)
