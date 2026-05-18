# FieldMedic Pocket — Documentation Index

Reference docs supporting the [FieldMedic Pocket](README.md) submission to the
Gemma 4 Good Hackathon (Kaggle × Google DeepMind, May 2026).

## Submission artifacts (start here)

| File | What's inside |
|---|---|
| [README.md](README.md) | Project overview, screenshots, build instructions |
| [SUBMISSION-WRITEUP.md](SUBMISSION-WRITEUP.md) | Long-form writeup — paste into the Kaggle submission form |
| [HUGGINGFACE-MODEL-CARD.md](HUGGINGFACE-MODEL-CARD.md) | Model card for the LoRA at huggingface.co/VishavGarg/fieldmedic-gemma-4-e2b-lora |
| [03-demo-video-script.md](03-demo-video-script.md) | Shot-by-shot script for the 90-second demo video |
| [LICENSE](LICENSE) | Apache 2.0 |

## Design + protocol references

| File | What's inside |
|---|---|
| [00-strategy.md](00-strategy.md) | Why this idea, what the hackathon rewards, target tracks |
| [01-mvp-scope.md](01-mvp-scope.md) | What ships in v1, what was deliberately cut |
| [02-triage-system-prompt.md](02-triage-system-prompt.md) | The system prompt for Gemma 4 E2B in production |
| [02-triage-function-schemas.json](02-triage-function-schemas.json) | The four router-function schemas (report_triage, escalate_emergency, request_retake, refuse_out_of_scope) |
| [02-triage-test-cases.md](02-triage-test-cases.md) | 20 held-out test cases for prompt validation |
| [04-tech-stack.md](04-tech-stack.md) | Tech-stack choices with explicit rejection rationale for alternatives |
| [09-handoff-card.md](09-handoff-card.md) | The offline summary card design — what makes FieldMedic look like a product, not a chatbot |
| [WHISPER-CPP-INTEGRATION.md](WHISPER-CPP-INTEGRATION.md) | How whisper.cpp is wired into the Android app for multilingual offline STT |

## Code

| Folder | What's inside |
|---|---|
| [gallery/Android/src/](gallery/Android/src/) | Android app (Kotlin / Compose) — fork of google-ai-edge/gallery with FieldMedic UI and Cactus integration |
| [unsloth/](unsloth/) | Training notebook, dataset, eval harness for the Gemma 4 E2B fine-tune |
| [whisper.cpp/](whisper.cpp/) | Speech-to-text submodule (vendored) for on-device multilingual STT |
| [gallery/Android/src/app/src/main/java/com/google/ai/edge/gallery/fieldmedic/cactus/CactusEngine.kt](gallery/Android/src/app/src/main/java/com/google/ai/edge/gallery/fieldmedic/cactus/CactusEngine.kt) | Cactus on-device runtime integration (Cactus prize track) |

## Hackathon quick-reference

- Competition: https://www.kaggle.com/competitions/gemma-4-good-hackathon
- Submission deadline: **2026-05-18**
- Prize pool: $200K (general + impact + technical categories)
- Special prizes targeted: **Cactus prize** (local-first mobile) + **Unsloth Special Technology Prize** ($10K)
- Tracks: Health & Sciences, Global Resilience, Safety/Responsible AI
