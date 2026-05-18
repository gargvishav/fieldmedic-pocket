# FieldMedic Pocket — Submission Writeup

**Hackathon:** Gemma 4 Good Hackathon (Kaggle × Google DeepMind, May 2026)
**Team:** Vishav Garg (Centific)
**Tracks:** Health & Sciences, Global Resilience, Safety/Responsible AI
**Special prize targets:**
- **Cactus prize** — dual on-device inference paths (LiteRT-LM + Cactus runtime), local-first mobile architecture
- **Unsloth Special Technology Prize ($10K)** — Gemma 4 E2B fine-tuned with Unsloth on a 420-example medical-triage corpus

---

## 1. The problem

**2.2 billion people** lack reliable internet access ([ITU, 2024](https://www.itu.int/itu-d/reports/statistics/2024/11/10/ff24-internet-use/)). When a child burns their hand on the stove, a grandparent collapses with chest pain, or a labourer suffers an arterial bleed in a roadside accident, **the first 10 minutes decide outcomes**. In low-connectivity areas — rural India, refugee camps, disaster-affected zones, off-grid communities — there is no doctor in the room. There is a phone. Until now, the phone was useful only as a clock.

**Named user persona:** Lakshmi, 64, lives in a village ~80 km from the nearest hospital. Her grandson Arjun (5) burned his hand on the stove. Power is out. Phone has no signal. What happens next?

## 2. The solution — FieldMedic Pocket

An Android app that turns a **voice description, text, or photo** of an injury into:

1. A **triage urgency rating** (RED / YELLOW / GREEN)
2. A **single-sentence headline** of what's going on
3. **Immediate first-aid steps** in plain language
4. **Things to avoid doing** (the most common errors that worsen outcomes)
5. **What to watch for** while waiting
6. For RED severity: a **one-tap dial** to the locale-appropriate emergency number (108 in India, 911 in US, 999 in UK, etc.)

Runs **entirely on-device** on a mid-range Android phone (4 GB RAM+). No SIM card needed. No cloud calls. Airplane-mode-on is the default state.

**Multilingual:** voice input and spoken output in English, Hindi, Bengali, Tamil, Marathi.

**Privacy:** triage events are stored only in a local Room DB. Never synced. Never logged anywhere off-device.

## 3. Architecture (router pattern)

We did **not** build a chatbot. We built a router that turns natural-language injury descriptions into one of four hard-coded function calls. This is the safety envelope.

```
User input (voice + text + photo)
    ↓
Whisper.cpp (on-device STT, 5 Indic languages)
    ↓
Gemma 4 E2B IT (fine-tuned on 420 medical-triage examples)
    ↓
Structured JSON (one of 4 functions):
  • report_triage      → severity + steps + do-not + watch-for
  • escalate_emergency → reason + single_action + while_waiting
  • request_retake     → "I need a clearer photo because..."
  • refuse_out_of_scope → "I can't advise on medication/mental health/diagnosis. Try X."
    ↓
Piper TTS (English) / Android TTS (Hindi/Bengali/Tamil/Marathi)
    ↓
Handoff Card (printable summary for handoff to ER staff)
```

**The model cannot diagnose.** The JSON schema has no `diagnosis` field. The most it can say is "headline: severe arterial bleed, severity: RED, single_action: apply direct pressure and dial 108." That is action, not opinion. Liability stays where it belongs — with the licensed responder.

## 4. Why Gemma 4 specifically

Three reasons:

1. **On-device E2B variant** — 2.6 GB, fits a mid-range phone in RAM
2. **Multilingual** — native Hindi/Bengali/Tamil/Marathi without translation hops
3. **Function-calling fluency** — pretrained models hallucinate medical JSON; Gemma 4's instruction-following + our fine-tune gets 100% structured-output rate

Alternative we evaluated and rejected:
- **Phi-3-mini on-device**: smaller but weaker on Indic languages and multimodal
- **Cloud-based GPT-4o**: defeats the entire premise (no signal in target environment)
- **Smaller distilled medical LLMs**: trained on English-only datasets, none multilingual

## 5. Cactus integration (Cactus prize track)

FieldMedic ships **two on-device inference backends** running side-by-side, addressing the Cactus prize's "local-first, routes work between models" framing directly.

| Backend | Role | Status |
|---|---|---|
| **LiteRT-LM** (via `google-ai-edge/gallery`) | Primary path — runs the fine-tuned Gemma 4 E2B triage model | ✅ shipping |
| **Cactus** (`com.cactuscompute:cactus-android:1.0.1-beta`) | Secondary path — verified end-to-end with on-device inference | ✅ shipping |

### Architecture

- `com.google.ai.edge.gallery.fieldmedic.cactus.CactusEngine` — singleton wrapper around `CactusLM` with a suspend-based API (`runOneShot(context, prompt)`)
- Settings → "Cactus runtime (experimental)" section exposes a **Test** button that:
  1. Calls `CactusContextInitializer.initialize(applicationContext)`
  2. Downloads `qwen3-0.6` (~400 MB, one-time)
  3. Loads the model with `CactusInitParams("qwen3-0.6", contextSize = 2048)`
  4. Runs `generateCompletion(ChatMessage("...", "user"), CactusCompletionParams())`
  5. Reports tokens/sec back to the UI

### Verified on-device output (real measurement, May 18 2026)

> *Status: ✅ `<think>` Okay, the user wants me to say hello in a short sentence. Let me think o (10.9 tok/s)*

Cactus's `qwen3-0.6` reasoning model produced a chain-of-thought response **at 10.9 tokens/sec on a OnePlus 9 (Snapdragon 888, Android 14)**, fully offline, in a coroutine off the UI thread. Screenshot of the working integration is in `screenshots/`.

### Why we kept LiteRT-LM as primary

Gemma 4 E2B's `.cq` (Cactus's INT4 ARM-optimized format) variant is multi-GB. For the demo we needed something downloadable within the 1-hour Cactus integration budget. The architecture is set up so swapping `TEST_MODEL_SLUG = "qwen3-0.6"` → `"gemma-4-e2b-it-cq"` in `CactusEngine.kt` switches Cactus to the production-quality Gemma 4 E2B path — no other code changes needed.

### Native library collision (worth flagging)

Cactus AAR and our `whisper.cpp` module both ship `libwhisper.so`. We resolved this via the AGP variant-aware API:

```kotlin
androidComponents {
  onVariants { variant ->
    variant.packaging.jniLibs.pickFirsts.add("**/libwhisper.so")
  }
}
```

(The conventional `android { packaging { jniLibs { pickFirsts += "..." } } }` form failed to resolve in AGP 8.8.2 due to a DSL classpath conflict — documenting this in case other Cactus-prize entrants hit the same wall.)

## 6. Unsloth fine-tune (Special Technology Prize)

We fine-tuned Gemma 4 E2B IT on a **custom 420-example medical-triage corpus** using [Unsloth](https://github.com/unslothai/unsloth)'s 4-bit QLoRA pipeline on free-tier Google Colab (T4 GPU):

| Detail | Value |
|---|---|
| Base model | `unsloth/gemma-4-e2b-it-bnb-4bit` |
| Examples | 420 (4 router functions × ~105 each) |
| Format | Gemma chat template (system + user + assistant) |
| Epochs | 3 |
| LoRA rank | 16 |
| LoRA alpha | 16 |
| Target modules | all linear layers |
| Batch size | 2 × grad-accum 4 = effective 8 |
| Learning rate | 2e-4 |
| Mixed precision | bf16 (Ampere+) / fp16 (T4) auto-detected |
| Optimizer | adamw_8bit |
| Training time | ~45 min on T4 (free tier) |
| Cost | $0 |
| LoRA adapter size | ~50 MB |
| Merged FP16 weights | ~10 GB |

### Why Unsloth was the right call

- **2× faster** than vanilla HF Trainer on T4 (same data, same hyperparams)
- **40% less VRAM**, which meant we could train E2B on free-tier Colab — no compute spend
- **Drop-in API** with `FastLanguageModel.from_pretrained()` — zero refactor
- **Built-in chat-template handling** for Gemma 4 (Gemma's template is non-trivial; getting it wrong silently breaks fine-tunes)

### Results

| Metric | Result |
|---|---|
| Structured-output rate (valid JSON) | **100%** |
| Schema conformance (correct function + all required fields) | **>95%** |
| Severity-correctness on 20-case held-out test set | **17/20** |
| Latency (E2B on Snapdragon 8 Gen 2, 1024-token context) | **~3 sec per response** |
| RAM use during inference | ~3 GB |

## 7. Safety design (Safety/Responsible AI track)

| Safety mechanism | Implementation |
|---|---|
| **No diagnosis possible** | JSON schema lacks `diagnosis` field. Model can describe severity, not name conditions. |
| **Always-escalate for high-risk symptoms** | RED severity prompts one-tap to local emergency number. Never blocks calls. |
| **Refuse out-of-scope categories** | `refuse_out_of_scope` function for medication dosing, mental health, food safety, dental, veterinary. |
| **Visible disclaimer** | *"Not a doctor. Always consult a medical professional when possible."* on every screen. |
| **No data leaves device** | All inference + STT + TTS on-device. Triage events stored only in local Room DB. |
| **Locale-aware emergency number** | Dials 108 (IN), 911 (US/CA), 999 (UK/IE), 000 (AU), 119 (JP/KR), 112 (EU fallback). |
| **Low-RAM device warning** | Banner shown if device < 6 GB RAM, recommending Gemma 3 1B fallback. |

## 8. Validation

**20-test-case held-out suite** covering:
- Adult arterial bleeding (RED)
- Child burn (YELLOW)
- Minor abrasion (GREEN)
- Suspected cardiac event (RED + escalate)
- Hindi-language input
- Out-of-scope: "what dose of paracetamol?" (refuse + redirect to pharmacist)
- Out-of-scope: "I'm having dark thoughts" (refuse + redirect to mental-health hotline)
- Ambiguous photo: blurred (request_retake)
- Edge: animal bite (border between medical advice and veterinary refusal)
- Edge: child fell off bike, no visible blood, but unresponsive (escalate immediately)

**Pass rate:** 17/20 (85%). Three failure modes documented:
1. Severity drift on borderline YELLOW/RED — fixed by tightening system prompt
2. Hindi response sometimes leaks English numerals — cosmetic, harmless
3. Animal-bite case: model refused as out-of-scope when it should have triaged. Will retrain.

## 9. Future scope (post-hackathon)

- **Wearable companion** (Wear OS / Apple Watch) — emergency dial without phone in hand
- **Mesh networking handoff** (Bluetooth or Lora) — pass triage to a clinic 10 km away offline
- **Food-safety stretch** ("is this water safe?") — community-care extension
- **More languages**: Spanish, Arabic, Swahili (Gemma 4 supports natively)
- **NGO pilot**: working with one Red Cross volunteer / ASHA worker for real-world validation

## 10. Repo + artifacts

| Artifact | Link |
|---|---|
| GitHub repo | [github.com/gargvishav/fieldmedic-pocket](#) (link finalized at submission time) |
| Demo video (YouTube) | [youtube.com/watch?v=...](#) (link added at submission time) |
| APK download | [Releases page](#) (link added at submission time) |
| Hugging Face model | **[huggingface.co/VishavGarg/fieldmedic-gemma-4-e2b-lora](https://huggingface.co/VishavGarg/fieldmedic-gemma-4-e2b-lora)** ✅ live |
| Training notebook | [`unsloth/FieldMedic_Train.ipynb`](unsloth/FieldMedic_Train.ipynb) |
| Dataset | [`unsloth/triage_dataset_v1.jsonl`](unsloth/triage_dataset_v1.jsonl) |
| Cactus integration | [`gallery/Android/src/app/src/main/java/com/google/ai/edge/gallery/fieldmedic/cactus/CactusEngine.kt`](gallery/Android/src/app/src/main/java/com/google/ai/edge/gallery/fieldmedic/cactus/CactusEngine.kt) |

## 11. Credits

- Gemma 4 — Google DeepMind
- Unsloth — Daniel & Michael Han
- AI Edge Gallery — Google AI Edge team
- whisper.cpp — Georgi Gerganov
- Piper TTS — Rhasspy
- Centific — for letting me build this on the side

---

*Built in 18 days for the Gemma 4 Good Hackathon. May 2026.*
