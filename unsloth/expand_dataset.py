"""Expand dataset_seed.jsonl into a larger training corpus via LLM paraphrase.

Strategy: for each seed example, ask a fast LLM to generate N paraphrases of the user
input that should map to the SAME JSON output.

Auto-picks a provider based on which env var is set:
    GROQ_API_KEY   → uses Groq + llama-3.3-70b-versatile (fast, generous free tier)
    GEMINI_API_KEY → uses Google Gemini Flash (slower free tier — 5-15 RPM)

Setup (Groq path — recommended):
    pip install groq
    export GROQ_API_KEY=gsk_...
    python expand_dataset.py

Setup (Gemini path):
    pip install google-generativeai
    export GEMINI_API_KEY=...
    python expand_dataset.py
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time

INPUT_FILE = "dataset_seed.jsonl"
OUTPUT_FILE = "dataset_expanded.jsonl"


class GroqClient:
    """Tiny Groq wrapper exposing a generate(prompt) -> str method."""

    def __init__(self, model: str = "llama-3.3-70b-versatile"):
        try:
            from groq import Groq  # noqa: F401
        except ImportError:
            sys.exit("groq SDK not installed. Run `pip install groq`.")
        from groq import Groq

        api_key = os.environ.get("GROQ_API_KEY")
        if not api_key:
            sys.exit(
                "Set GROQ_API_KEY in your environment. "
                "Get one free at https://console.groq.com/keys"
            )
        self.client = Groq(api_key=api_key)
        self.model = model

    def generate(self, prompt: str) -> str:
        resp = self.client.chat.completions.create(
            model=self.model,
            messages=[{"role": "user", "content": prompt}],
            temperature=0.7,
            max_tokens=512,
        )
        return resp.choices[0].message.content or ""


class GeminiClient:
    """Gemini fallback. Slower free tier than Groq."""

    def __init__(self):
        try:
            import google.generativeai as genai
        except ImportError:
            sys.exit(
                "google-generativeai not installed. Run `pip install google-generativeai`."
            )
        api_key = os.environ.get("GEMINI_API_KEY")
        if not api_key:
            sys.exit("Set GEMINI_API_KEY or GROQ_API_KEY.")
        genai.configure(api_key=api_key)
        candidates = [
            "models/gemini-2.0-flash",
            "models/gemini-2.0-flash-exp",
            "models/gemini-1.5-flash-latest",
            "models/gemini-1.5-flash",
            "models/gemini-2.5-flash",
            "models/gemini-pro",
        ]
        last_err: Exception | None = None
        for name in candidates:
            try:
                model = genai.GenerativeModel(name)
                _ = model.generate_content("ok").text
                self.model = model
                self.name = name
                print(f"Using Gemini model: {name}")
                return
            except Exception as e:  # noqa: BLE001
                last_err = e
        sys.exit(f"No usable Gemini model. Last error: {last_err}")

    def generate(self, prompt: str) -> str:
        return self.model.generate_content(prompt).text or ""


def _get_client():
    """Auto-pick provider based on which env var is set."""
    if os.environ.get("GROQ_API_KEY"):
        c = GroqClient()
        print(f"Using Groq model: {c.model}")
        return c
    if os.environ.get("GEMINI_API_KEY"):
        return GeminiClient()
    sys.exit(
        "No API key found. Set either GROQ_API_KEY or GEMINI_API_KEY in your environment."
    )


def _paraphrase(client, user_text: str, n: int, max_retries: int = 4) -> list[str]:
    prompt = f"""You are augmenting a first-aid triage training dataset.

Paraphrase the message below into {n} natural variations someone might actually say in panic, in a hurry, or casually — including with typos, missing words, or different phrasing. KEEP the medical facts EXACTLY the same: same body part, same severity cue, same situation. Don't add new symptoms or remove the urgency.

Output exactly {n} lines, one paraphrase per line. No numbering, no quotes, no preamble.

Original: "{user_text}"

Paraphrases:"""
    last_err: Exception | None = None
    for attempt in range(max_retries):
        try:
            text = client.generate(prompt)
            lines = [
                line.strip().lstrip("0123456789.-) \t").strip('"').strip()
                for line in text.split("\n")
                if line.strip()
            ]
            lines = [line for line in lines if line and len(line) >= 4]
            if len(lines) >= 1:
                return lines[:n]
        except Exception as e:  # noqa: BLE001
            last_err = e
            err_str = str(e)
            wait = 3.0 * (attempt + 1)
            if "429" in err_str or "rate" in err_str.lower():
                wait = max(wait, 30.0)
            time.sleep(wait)
    print(f"WARN: failed to paraphrase after {max_retries} retries: {last_err}", file=sys.stderr)
    return []


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--paraphrases", type=int, default=4, help="paraphrases per seed")
    parser.add_argument(
        "--rate-limit-sec",
        type=float,
        default=2.0,
        help="seconds between API calls. Groq free tier ≈ 30 RPM, Gemini free ≈ 5-15 RPM.",
    )
    parser.add_argument("--input", default=INPUT_FILE)
    parser.add_argument("--output", default=OUTPUT_FILE)
    args = parser.parse_args()

    if not os.path.exists(args.input):
        sys.exit(f"Missing {args.input}. Are you in the unsloth/ directory?")

    client = _get_client()

    out_entries: list[dict] = []
    with open(args.input) as f:
        seeds = [json.loads(line) for line in f if line.strip()]

    print(f"Loaded {len(seeds)} seeds. Generating {args.paraphrases} paraphrases each...")

    for i, seed in enumerate(seeds, start=1):
        user_msg = next(m for m in seed["messages"] if m["role"] == "user")
        assistant_msg = next(m for m in seed["messages"] if m["role"] == "assistant")
        original = user_msg["content"]

        # Always include the original.
        out_entries.append(
            {
                "messages": [
                    {"role": "user", "content": original},
                    {"role": "assistant", "content": assistant_msg["content"]},
                ]
            }
        )

        variants = _paraphrase(client, original, args.paraphrases)
        for v in variants:
            if v.strip().lower() == original.strip().lower():
                continue  # skip duplicates of the original
            out_entries.append(
                {
                    "messages": [
                        {"role": "user", "content": v},
                        {"role": "assistant", "content": assistant_msg["content"]},
                    ]
                }
            )
        print(f"  [{i}/{len(seeds)}] {original[:60]!r:<62} → {len(variants)} paraphrases")
        time.sleep(args.rate_limit_sec)

    with open(args.output, "w") as f:
        for entry in out_entries:
            f.write(json.dumps(entry, ensure_ascii=False) + "\n")
    print(f"\nWrote {len(out_entries)} examples to {args.output}")
    print(f"Train with: python train.py  (set DATASET_PATH = '{args.output}' in train.py)")


if __name__ == "__main__":
    main()
