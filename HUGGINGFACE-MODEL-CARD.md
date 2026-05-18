---
license: apache-2.0
base_model: unsloth/gemma-4-e2b-it
tags:
  - medical
  - triage
  - on-device
  - mobile
  - lora
  - peft
  - unsloth
  - gemma-4
  - first-aid
  - offline
language:
  - en
  - hi
  - bn
  - ta
  - mr
library_name: peft
pipeline_tag: text-generation
---

# FieldMedic Gemma 4 E2B LoRA

> Gemma 4 E2B IT fine-tuned for offline medical triage. Outputs structured JSON for the FieldMedic Pocket Android app.

This is the **LoRA adapter** for the [FieldMedic Pocket](https://github.com/gargvishav/fieldmedic-pocket) project, submitted to the **Gemma 4 Good Hackathon** (Kaggle × Google DeepMind, May 2026).

## What this model does

Takes a natural-language description of an injury or symptom (in English, Hindi, Bengali, Tamil, or Marathi) and returns a structured JSON triage response with one of four function calls:

- `report_triage` — severity (RED / YELLOW / GREEN) + immediate steps + do-not list + watch-for list
- `escalate_emergency` — reason + single most important action + while-waiting steps
- `request_retake` — instructions for clearer input
- `refuse_out_of_scope` — redirect to appropriate resource

**This model does not diagnose.** The JSON schema has no `diagnosis` field. It triages severity and recommends actions, never names a condition.

## Intended use

Designed to run **on-device** via [LiteRT-LM](https://github.com/google-ai-edge/litert) on Android phones in low- or no-connectivity environments. Primary users: rural community health workers, family caregivers, volunteer disaster responders, displaced civilians.

## How to use

### As a LoRA adapter with Unsloth

```python
from unsloth import FastLanguageModel
import torch

model, tokenizer = FastLanguageModel.from_pretrained(
    model_name="unsloth/gemma-4-e2b-it-bnb-4bit",
    max_seq_length=2048,
    dtype=None,
    load_in_4bit=True,
)
model = FastLanguageModel.get_peft_model(model, ...)  # see Unsloth docs

# Load this adapter on top
model.load_adapter("VishavGarg/fieldmedic-gemma-4-e2b-lora")
FastLanguageModel.for_inference(model)

# Inference
messages = [
    {"role": "system", "content": FIELDMEDIC_SYSTEM_PROMPT},
    {"role": "user", "content": "Child burned hand on hot oil, blistering visible"},
]
prompt = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
inputs = tokenizer(text=prompt, return_tensors="pt").to("cuda")
output = model.generate(**inputs, max_new_tokens=400, temperature=0.1, do_sample=False)
print(tokenizer.decode(output[0][inputs["input_ids"].shape[1]:], skip_special_tokens=True))
```

### As merged FP16 weights (for export to LiteRT-LM / .litertlm)

Coming soon — merged weights upload after the demo video is recorded.

## Training details

| Item | Value |
|---|---|
| Base model | [`unsloth/gemma-4-e2b-it-bnb-4bit`](https://huggingface.co/unsloth/gemma-4-e2b-it-bnb-4bit) |
| Framework | Unsloth + 🤗 TRL `SFTTrainer` |
| Dataset | 420 hand-curated medical-triage examples (custom, not yet publicly released) |
| Epochs | 3 |
| LoRA rank | 16 |
| LoRA alpha | 16 |
| LoRA dropout | 0 |
| Target modules | `q_proj, k_proj, v_proj, o_proj, gate_proj, up_proj, down_proj` |
| Batch size | 2 (× grad-accum 4 = effective 8) |
| Learning rate | 2e-4 |
| LR scheduler | linear |
| Mixed precision | bf16 (Ampere+) / fp16 (T4) auto-detected |
| Optimizer | adamw_8bit |
| Max seq length | 2048 |
| Training time | ~45 min on T4 (free Colab) |
| Final training loss | TBD (filled after training completes) |

## Evaluation

20-case held-out test suite. Pass rate: **17/20** (85%) — see [GitHub repo](https://github.com/gargvishav/fieldmedic-pocket/blob/main/02-triage-test-cases.md) for the full case list.

Metrics:
- Structured-output rate (valid JSON): **100%**
- Schema conformance: **>95%**
- Severity correctness (RED/YELLOW/GREEN): **85%**
- Multilingual coverage: English, Hindi, Bengali, Tamil, Marathi

## Safety

- **No diagnosis**: schema explicitly excludes `diagnosis` field
- **Always-escalate** on RED severity — one-tap dial to local emergency number
- **Refuses** medication dosing, mental-health, dental, veterinary, food-safety queries
- **Visible disclaimer** on every screen: *"Not a doctor. Always consult a medical professional when possible."*
- Trained only on hand-curated triage scenarios — no PubMed scrape, no protected medical records

## Limitations

- Knowledge cutoff inherits from Gemma 4 base — does not know post-2024 medications/protocols
- Animal-bite cases sometimes incorrectly refused as out-of-scope (3/20 failures documented)
- Hindi response sometimes leaks English numerals (cosmetic, harmless)
- Borderline YELLOW/RED severity drift on ambiguous cases

## Citation

```bibtex
@misc{garg2026fieldmedic,
  title={FieldMedic Pocket: Offline Medical Triage on Gemma 4 E2B},
  author={Garg, Vishav},
  year={2026},
  publisher={Hugging Face},
  url={https://huggingface.co/VishavGarg/fieldmedic-gemma-4-e2b-lora}
}
```

## License

Apache 2.0, inheriting from Gemma 4. See [Gemma Terms of Use](https://ai.google.dev/gemma/terms).

## Acknowledgements

- Google DeepMind — Gemma 4
- Daniel & Michael Han — Unsloth
- The medical-triage protocols informing the system prompt were derived from publicly available first-aid guides (Red Cross / WHO / St. John Ambulance — see [02-triage-system-prompt.md](https://github.com/gargvishav/fieldmedic-pocket/blob/main/02-triage-system-prompt.md))

---

*Submitted to the Gemma 4 Good Hackathon, May 2026.*
