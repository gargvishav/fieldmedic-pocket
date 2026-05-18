# FieldMedic Gemma 4 E2B fine-tune (Unsloth)

This directory holds everything needed to fine-tune Gemma 4 E2B on FieldMedic triage data and ship the result back to the on-device app.

**Prize target:** Unsloth Special Technology Prize ($10K) — *"the best fine-tuned Gemma 4 model created using Unsloth, optimized for a specific, impactful task."*

## What this does

We take Gemma 4 E2B (the same model that runs on the OnePlus 9) and fine-tune it on a small, hand-crafted dataset of first-aid triage prompt → JSON-response pairs. The result is a model that produces our exact JSON schema with higher reliability, fewer drift errors (`escalation_emergency` vs `escalate_emergency`), and tighter adherence to severity rules.

## Hardware required

- Cloud GPU with 16+ GB VRAM (A100, T4 doesn't quite cut it for 4B params)
- Or local GPU rig (RTX 3090/4090/4080)
- We use 4-bit QLoRA via Unsloth so 4B model fits in ~10 GB VRAM

Recommended: Google Colab Pro / Kaggle Notebooks (free GPU access for the hackathon scope).

## Files

| File | Purpose |
|---|---|
| `dataset_seed.jsonl` | Hand-crafted training examples in chatml format (~50 pairs) |
| `expand_dataset.py` | Augments the seed set with Gemma-3-generated variations to reach 200-500 pairs |
| `train.py` | Unsloth + TRL SFT trainer; outputs LoRA adapter |
| `merge_and_export.py` | Merges LoRA into base, exports to LiteRT-LM format for Android |
| `eval.py` | Runs the 20 test cases from `02-triage-test-cases.md` against the fine-tuned model and reports pass rate |
| `requirements.txt` | pip deps |

## Workflow (estimated 1 day end-to-end)

```
1. python expand_dataset.py            # ~30 min Gemma-3 API calls
2. python train.py                      # ~2-4 hours on Colab T4 (longer for A100 if reasoning steps high)
3. python eval.py                       # ~10 min — must hit ≥90% on our test cases
4. python merge_and_export.py           # ~30 min — produces .litertlm
5. adb push fieldmedic-gemma-4.litertlm /sdcard/.../files/...   # drop into the app
```

## Pass criteria

The fine-tuned model must:
- Hit ≥90% on the 20 triage test cases (vs 87% for prompt-only Gemma 4 E2B)
- Produce valid JSON 100% of the time (no prose pollution)
- Use exact function names (`escalate_emergency`, not `escalation_emergency`)
- Maintain Hindi/Bengali/Tamil/Marathi response capability when language directive is present

## What we DO NOT change

- Model size: still E2B (so it keeps running on the OnePlus 9)
- Tokenizer
- System prompt structure (the prompt is part of training input)
- Gallery integration (drop-in replacement of the .litertlm file)

## Reverting

If the fine-tune underperforms, swap back to upstream `gemma-4-E2B-it.litertlm`. Both files coexist in `/sdcard/Android/data/com.google.aiedge.gallery/files/`.
