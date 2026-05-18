# Strategy — Win Playbook

## The hackathon

- **Name:** The Gemma 4 Good Hackathon (Kaggle x Google DeepMind)
- **Deadline:** 2026-05-18
- **Prize pool:** $200,000 (general + impact + technical categories)
- **Special prize to target:** **Cactus prize** — explicitly rewards local-first mobile/wearable apps that route work between models. Smaller competitor pool, testable criteria. **FieldMedic is built for it.**
- **Submission package required:** working demo + public code repo + technical writeup + short video + media gallery
- **Primary judging surface:** the video demo

## What the hackathon rewards (signal)

1. **Artifact quality over novelty theater** — demo must be end-to-end, not a slide deck
2. **Local-first engineering** — visibly offline, on-device, privacy-preserving
3. **Multimodal, workflow-specific usefulness** — one painful task done very well
4. **Differentiation against visible crowding** — medical-doc summarizer / generic crisis chatbot / offline tutor are already crowded in public notebooks; need a tighter wedge

Past winners (Gemma 3n, MedGemma) consistently rewarded **narrow, mobile-first tools with one named beneficiary and a concrete before/after**. Gemma Vision (blind nav), Vite Vere Offline (cognitive support), EpiCast (outbreak), FieldScreen AI (TB), BridgeDX (offline triage).

## The locked idea

**FieldMedic Pocket** — offline first-aid triage app that turns a photo + voice description into urgency rating (red/yellow/green) and step-by-step first-aid until trained help arrives.

**Target tracks:** Health & Sciences + Global Resilience + Safety/Responsible AI.

**User personas:** family caregivers in low-connectivity areas, rural community health workers, volunteer responders during outages/disasters, displaced civilians.

## Decision history (why this idea)

Considered four ideas:
1. Survival AI Companion (broad: injury + food + nav)
2. Voice2Action (WhatsApp voice control)
3. Braille AI Reader
4. MedLabel (prescription explainer)

**Briefly recommended MedLabel** — dropped after research showed visible public notebooks (MedLit, medical-doc summarizers) already in the competition. Crowded category.

**Briefly objected to Survival on offline grounds** — wrong; Gemma 4 has E2B/E4B small variants explicitly for on-device deployment via LiteRT-LM. Cactus prize *rewards* local-first.

**Final:** Survival reframed as FieldMedic Pocket — civilian/humanitarian framing, narrow scope (triage + escalate, NOT diagnosis), liability minimized.

## Win playbook — 5 highest-leverage moves

### 1. Target the Cactus prize, not just the main track
Smaller competitor pool, criteria are explicit. FieldMedic is born for it. Architect to those criteria specifically. Submit for both Cactus and main track.

### 2. The video must be a film, not a screen recording
- Real phone, real hands, real setting (rural kitchen / dark room / roadside)
- **Film the airplane-mode toggle on camera** — that's the offline story made visible
- One named person, real story
- No robot TTS in the marketing video — hire a real voice ($20 on Fiverr)
- Aim for ad-quality, not demo-quality

### 3. Hindi + English voice from day one
Gemma 4 small models support native audio. Demoing in Hindi tells Google DeepMind judges "real impact for billions" without saying it. Most teams ship English-only — free differentiation.

### 4. Cut features, not polish
Polish > features at hackathons. If behind on day 10: cut photo before voice. Voice + handoff card alone can win. Photo without voice cannot.

## Three failure modes to avoid

1. **Demo looks like a screen recording** → judges scroll past in 10 seconds
2. **"Offline" claim is a lie** (cloud API hidden behind UI) → judges check, eliminated
3. **Too generic** ("AI helps in emergencies") → blends into 200 similar entries

## Architecture pattern (locked)

**Router architecture** — matches Cactus prize wording:
- Tiny perception model or deterministic local tool handles input cheaply
- **Gemma 4 E2B** does the main reasoning pass (default)
- Native function calling turns reasoning into device actions
- E4B reserved for optional deeper reasoning on stronger hardware

## Stretch goals (not for v1)

- Multilingual: Hindi (must) + Bengali / Tamil / Spanish (stretch)
- Wearable companion (Cactus loves this)
- Mesh networking for clinic handoff
- LoRA fine-tune on first-aid corpus (only if v1 ships early)
