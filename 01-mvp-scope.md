# MVP Scope (LOCKED)

## What ships in v1

### Feature 1: Photo injury triage
User snaps a photo of an injury. App returns:
- 🔴 RED / 🟡 YELLOW / 🟢 GREEN urgency
- 3–5 immediate steps
- 1–3 things to NOT do
- Escalation triggers ("watch for X")

### Feature 2: Voice-first first-aid Q&A
User speaks: *"Someone is bleeding from the leg, what do I do?"*
App responds with spoken step-by-step first-aid (hands-free, panic-friendly).

### Feature 3: Offline handoff card
When help arrives, app shows a structured summary:
- What happened, when
- Photo (if taken)
- What the helper did
- Vitals if entered
- Auto-generated factual notes for paramedic / family

## What's CUT from v1

| Feature | Why cut |
|---|---|
| Food safety | Liability — spoiled food often looks fine; one bad call = poisoning |
| Navigation / survival guide | Duplicates Google Maps offline + survival PDFs; not a wedge |
| Disease diagnosis | Out of scope; triages urgency, not conditions |
| Drug dosing / pharmacology | High risk; never recommend by name/dose |
| Mental health crisis support | Needs human, not AI |
| Pet / animal injuries | Out of scope |

## Stretch (only if v1 ships by day 11)

| Feature | Priority |
|---|---|
| Hindi voice support | HIGH (judges from Google DeepMind) |
| Function-calling tools (timer, flashlight) | MEDIUM |
| Multiple Indian languages | LOW |

## Cut criteria (if behind schedule)

If by **day 10** the demo doesn't run end-to-end on a phone:

1. First cut: Photo feature → voice + handoff card alone is the MVP
2. Second cut: Function-calling tools → just generate text + speak it
3. Third cut: Reduce demo to one user persona + one scenario

## Hard constraints

- **Must run fully offline** (airplane mode demo non-negotiable)
- **Must run on a real consumer Android phone** (not an emulator, not a cloud)
- **Must use Gemma 4** (E2B default, E4B fallback) — this is the hackathon point
- **Must NOT diagnose or prescribe** — triage urgency only
- **Must always include escalation trigger** — even on GREEN cases
