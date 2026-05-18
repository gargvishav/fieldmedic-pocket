"""Merge the LoRA adapter into the base Gemma 4 E2B and export to LiteRT-LM.

Outputs: fieldmedic-gemma-4-e2b.litertlm — drop into the on-device app at
/sdcard/Android/data/com.google.aiedge.gallery/files/Gemma_4_E2B_it/<hash>/

Note: LiteRT-LM export requires `ai-edge-torch` from Google
(https://github.com/google-ai-edge/ai-edge-torch). At time of writing the public
API for converting fine-tuned Gemma 4 weights → .litertlm is still maturing; if
the export call below fails, see the ai-edge-torch examples for the current path.
"""

from __future__ import annotations

import os

from unsloth import FastLanguageModel

ADAPTER_DIR = "fieldmedic-gemma-4-e2b-lora"
MERGED_DIR = "fieldmedic-gemma-4-e2b-merged"
LITERTLM_OUT = "fieldmedic-gemma-4-e2b.litertlm"


def main() -> None:
    model, tokenizer = FastLanguageModel.from_pretrained(
        model_name=ADAPTER_DIR,
        max_seq_length=4096,
        dtype=None,
        load_in_4bit=False,  # need full precision to merge cleanly
    )
    # Saves the merged 16-bit weights — fine to ship to a converter.
    model.save_pretrained_merged(MERGED_DIR, tokenizer, save_method="merged_16bit")
    print(f"Merged HF model written to {MERGED_DIR}")

    # LiteRT-LM export.
    # Pseudocode — replace with the current ai-edge-torch invocation:
    #   from ai_edge_torch.generative.examples.gemma3 import gemma3
    #   converter = gemma3.build_e2b_4b_model(
    #       checkpoint_path=MERGED_DIR,
    #       kv_cache_max_len=4096,
    #   )
    #   converter.export(LITERTLM_OUT)
    print("Run ai-edge-torch's Gemma 4 export to produce", LITERTLM_OUT)
    print("See https://github.com/google-ai-edge/ai-edge-torch/tree/main/ai_edge_torch/generative/examples")


if __name__ == "__main__":
    main()
