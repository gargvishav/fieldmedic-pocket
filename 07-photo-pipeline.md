# Photo Triage Pipeline

**Status:** TBD — built after baseline Gemma 4 E2B is working with text.

## What this file will contain

- CameraX integration (capture single photo, not video)
- Image preprocessing (resize, compress, color-correct for low-light)
- Multimodal input format for Gemma 4 E2B (image + text prompt)
- Pipeline: photo → optional tiny pre-filter → Gemma 4 E2B → function-call response → renderer
- Error handling (blurry photo → request_retake function)

## Decisions still open

- Use a **tiny pre-filter** (e.g., is-this-an-injury classifier) or send straight to Gemma?
  - Pro: filters obvious non-injuries cheaply
  - Con: extra model = more battery/memory
  - Default: skip pre-filter for v1, add only if Gemma misclassifies too often

- Single snapshot vs continuous frames?
  - **Single snapshot** (per portfolio research — multimodal small models work best on single images, not video)

## Inputs/outputs

```
Input:  JPEG image (1024x1024 max) + optional voice transcript
Output: function call (one of report_triage / request_retake / escalate / refuse)
```

## Pass criteria for "done"

- Photo of small cut → returns valid GREEN report_triage
- Blurry photo → returns request_retake
- Photo of food → returns refuse_out_of_scope
- End-to-end latency <8 seconds on target phone
