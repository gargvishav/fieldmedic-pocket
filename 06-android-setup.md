# Android Project Setup

**Status:** TBD — fills in once tech stack is chosen.

## What this file will contain

- Repo skeleton: `app/`, `model/`, `prompts/`, `tests/`
- `build.gradle` dependencies (Kotlin, Compose, CameraX, Room, LiteRT-LM)
- Model loading code (Gemma 4 E2B from local assets vs downloaded)
- App architecture diagram (4-function router → renderer)
- Permissions manifest (camera, mic, storage)
- Day-1 baseline: hello-world prompt running on device

## Day-1 baseline pass criteria

- App installs on real phone
- Loads Gemma 4 E2B model into memory
- Sends "Hello, are you working?" → gets a response
- Response time logged for benchmarking
- Works in airplane mode

## Reference samples to start from

- LiteRT-LM Android sample (TBD link from ai.google.dev)
- AI Edge Gallery reference app (Google's official)
- ML Kit Prompt API quickstart

## Pending

- Confirm phone model
- Choose runtime (see 04-tech-stack.md)
