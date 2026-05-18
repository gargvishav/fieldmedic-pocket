"""FieldMedic Gemma 4 E2B fine-tune via Unsloth + TRL.

Run on a Colab/Kaggle GPU notebook (T4/A100/L4) — needs ~10-12 GB VRAM with 4-bit QLoRA.

Usage:
    pip install -r requirements.txt
    python train.py
"""

from __future__ import annotations

import os

from datasets import load_dataset
from transformers import TrainingArguments
from trl import SFTTrainer
from unsloth import FastLanguageModel

MODEL_NAME = "google/gemma-4-E2B-it"
MAX_SEQ_LEN = 4096
DATASET_PATH = (
    "dataset_expanded.jsonl"
    if os.path.exists("dataset_expanded.jsonl")
    else "dataset_seed.jsonl"
)
OUTPUT_DIR = "outputs"
ADAPTER_OUT = "fieldmedic-gemma-4-e2b-lora"


def main() -> None:
    model, tokenizer = FastLanguageModel.from_pretrained(
        model_name=MODEL_NAME,
        max_seq_length=MAX_SEQ_LEN,
        dtype=None,
        load_in_4bit=True,
    )

    # Apply LoRA on the standard attention + MLP projection matrices.
    model = FastLanguageModel.get_peft_model(
        model,
        r=16,
        target_modules=[
            "q_proj",
            "k_proj",
            "v_proj",
            "o_proj",
            "gate_proj",
            "up_proj",
            "down_proj",
        ],
        lora_alpha=16,
        lora_dropout=0.0,
        bias="none",
        use_gradient_checkpointing="unsloth",
        random_state=3407,
    )

    dataset = load_dataset("json", data_files=DATASET_PATH, split="train")

    def fmt(example):
        text = tokenizer.apply_chat_template(
            example["messages"], tokenize=False, add_generation_prompt=False
        )
        return {"text": text}

    dataset = dataset.map(fmt)

    trainer = SFTTrainer(
        model=model,
        tokenizer=tokenizer,
        train_dataset=dataset,
        dataset_text_field="text",
        max_seq_length=MAX_SEQ_LEN,
        packing=False,
        args=TrainingArguments(
            per_device_train_batch_size=2,
            gradient_accumulation_steps=4,
            warmup_steps=10,
            num_train_epochs=3,
            learning_rate=2e-4,
            fp16=False,
            bf16=True,
            logging_steps=1,
            optim="adamw_8bit",
            weight_decay=0.01,
            lr_scheduler_type="linear",
            seed=3407,
            output_dir=OUTPUT_DIR,
            report_to="none",
        ),
    )
    trainer.train()

    model.save_pretrained(ADAPTER_OUT)
    tokenizer.save_pretrained(ADAPTER_OUT)
    print(f"LoRA adapter saved to {ADAPTER_OUT}")
    print("Next: run merge_and_export.py to produce a .litertlm file.")


if __name__ == "__main__":
    main()
