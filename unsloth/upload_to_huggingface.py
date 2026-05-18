"""
Upload the trained LoRA adapter to Hugging Face.

Run this from Google Colab AFTER training cell 10 has finished successfully
and the adapter is saved to Drive.

Usage:
  1. In a fresh Colab cell, set:
       HF_TOKEN = "hf_..."   (get from https://huggingface.co/settings/tokens, "write" scope)
       HF_USERNAME = "VishavGarg"
  2. Run this script in a new cell:
       !python upload_to_huggingface.py
     OR paste the contents into a Colab cell directly.
"""

import os
from huggingface_hub import HfApi, login

# -------- CONFIG (edit these) --------
HF_USERNAME = os.environ.get("HF_USERNAME") or "VishavGarg"
HF_TOKEN = os.environ.get("HF_TOKEN")  # set via Colab Secrets or os.environ
REPO_NAME = "fieldmedic-gemma-4-e2b-lora"
LORA_DIR = "/content/drive/MyDrive/FieldMedic/fieldmedic-gemma-4-e2b-lora"
MODEL_CARD_PATH = "/content/drive/MyDrive/FieldMedic/HUGGINGFACE-MODEL-CARD.md"
# -------------------------------------

assert HF_TOKEN, "Set HF_TOKEN in os.environ before running."
assert os.path.isdir(LORA_DIR), f"LoRA folder missing at {LORA_DIR}"

login(token=HF_TOKEN)
api = HfApi()

repo_id = f"{HF_USERNAME}/{REPO_NAME}"
print(f"Creating / updating repo: {repo_id}")
api.create_repo(repo_id=repo_id, repo_type="model", exist_ok=True, private=False)

print(f"Uploading folder: {LORA_DIR}")
api.upload_folder(
    folder_path=LORA_DIR,
    repo_id=repo_id,
    repo_type="model",
    commit_message="Add FieldMedic Gemma 4 E2B LoRA adapter (420 examples, 3 epochs).",
)

# Upload README as the model card (HF auto-renders README.md as the card).
if os.path.isfile(MODEL_CARD_PATH):
    print(f"Uploading model card from {MODEL_CARD_PATH}")
    api.upload_file(
        path_or_fileobj=MODEL_CARD_PATH,
        path_in_repo="README.md",
        repo_id=repo_id,
        repo_type="model",
        commit_message="Add model card.",
    )

print(f"\n✅ Done. View at: https://huggingface.co/{repo_id}")
