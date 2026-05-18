# Demo Video Script — 90 seconds

**Status:** Ready to record. Total length target: 60-90 seconds. Filmed in landscape 1080p.

## What you need to record

1. **Phone in landscape**, brightness up, plugged in (battery is the enemy)
2. **Quiet room** for voiceover (or use the on-device TTS playback as soundtrack — saves a take)
3. **A second phone or laptop webcam** to film the phone screen (or use scrcpy + Mac screen record for cleanest)
4. **One real-world prop** if possible — a kitchen, a roadside, a bandage on a hand. Makes the demo a *film*, not a screencast.

---

## Shot list

### Shot 1 — Cold open (0:00 – 0:08, 8 sec)

**Visual:** Hands holding the phone. Airplane-mode toggle visible in status bar. The user (you) **toggles airplane mode ON** on camera. Status bar updates to "no signal."

**Voiceover (or on-screen text):**
> *"No signal. No internet. Just a phone."*

**Why this shot first:** This is the most important second of the video. It proves the "offline" claim before any logo, any branding, any UI. Past Kaggle winners (Gemma Vision, EpiCast) all opened with the *constraint* before the product.

### Shot 2 — Problem statement (0:08 – 0:18, 10 sec)

**Visual:** A wide shot of the user's hands or environment — a child's hand with a small (fake / makeup) burn mark, or a knee with a (fake) scrape. Camera pans up to the phone.

**Voiceover:**
> *"2.2 billion people live without reliable internet. When they need medical help, they have a phone — but the phone doesn't help."*

Optional alt voiceover if you don't want to quote ITU:
> *"In a real emergency in rural India, the nearest doctor can be 80 kilometers away. Phones don't help when there's no signal."*

### Shot 3 — Tap FieldMedic, ask a question (0:18 – 0:35, 17 sec)

**Visual:** Close-up of phone screen. Tap FieldMedic tile (now: instant entry to chat). Tap mic button. Speak naturally:

**You speak (or your prop "user" speaks):**
> *"My grandson burned his hand on the stove. There are blisters. He is crying. What do I do?"*

**Wait for Whisper to transcribe.** It appears on screen.

### Shot 4 — Gemma responds (0:35 – 0:55, 20 sec)

**Visual:** Gemma 4 streams the structured response. The handoff card appears below the message. Pull focus to:
- Severity chip: **YELLOW** in yellow
- Headline: *"Second-degree burn — cool and protect the wound"*
- Immediate steps: 1. *Run cool water over the burn for 10 minutes...* 2. *Cover with clean cloth...* 3. *Do not pop the blisters...*
- Watch for: *Signs of infection (redness spreading, fever) over the next 24 hours*

**Audio:** Let the Android TTS / Piper voice speak the response. The robot voice is FINE for the demo — it's the *offline* story. (Don't dub a real voice over — that breaks the "everything on-device" claim.)

### Shot 5 — Show airplane mode is still on (0:55 – 1:00, 5 sec)

**Visual:** Cut to status bar zoom. Airplane mode icon still visible. Move to home screen. Re-open FieldMedic — still works.

**Voiceover:**
> *"No internet was used. Ever."*

### Shot 6 — One-tap escalate (1:00 – 1:15, 15 sec) — OPTIONAL but high-impact

**Visual:** Go back. Type a new prompt: *"My father has chest pain shooting down his left arm and he can't breathe."* Gemma responds with **RED** severity and shows the single_action: *"Dial 108 now. While waiting, sit him upright and loosen tight clothes."* — and a big red phone button.

Tap the red phone. The Android dialer opens with **108** (or 911, depending on locale) pre-filled. **Don't actually call** — just show the dialer.

**Voiceover:**
> *"For emergencies, one tap dials the local emergency number. Locale-aware. Even offline."*

### Shot 7 — Tech credit + outro (1:15 – 1:30, 15 sec)

**Visual:** Black card. Three logo rows.
- Gemma 4 E2B (fine-tuned with Unsloth)
- Whisper.cpp + Piper TTS
- Runs entirely on-device

**Voiceover (final beat):**
> *"FieldMedic Pocket. Gemma 4 in your pocket — for the 2.2 billion people who need it most."*

End card: github URL + hugging face URL + your name.

---

## Recording mechanics

### Option A: Real-world film (best, more effort)

- One phone in landscape running FieldMedic
- One phone or DSLR filming
- Quiet room or outdoor (no traffic noise during voiceover)
- Use a second take just for the voiceover, layer on top
- Edit in DaVinci Resolve (free) or CapCut (mobile, free)

### Option B: Mac screen record via scrcpy (faster)

- Connect phone to Mac via ADB-over-WiFi
- Run scrcpy with `--max-size 1080`
- Use macOS built-in screen recording (Cmd+Shift+5) → record the scrcpy window
- Drive the phone via Mac mouse so you don't shake the camera
- Voice over later

### Option C: Just record on the phone (fastest, lowest quality)

- Android: Settings → System → Screen recording — built into Android 11+
- Hit record, demo, save
- Add voiceover in CapCut afterwards
- Acceptable as a backup, NOT the primary submission video

---

## Things to NOT do

❌ **Don't use a real voice actor.** It contradicts the "on-device TTS" story. The robot voice is the demo.

❌ **Don't speed up the video.** The 3-second Gemma latency on E2B is part of the "this actually works on a phone" credibility. Don't hide it.

❌ **Don't include a "join my discord" outro.** Hackathon judges don't care.

❌ **Don't film yourself at a desk.** Take it to a kitchen, a roadside, a hospital lobby waiting room. Past winners always filmed in context.

❌ **Don't claim a feature you didn't ship.** If photo input is unstable, cut it. If multilingual didn't make it, don't claim it.

---

## Upload destination

- **Primary:** YouTube (unlisted is fine, public preferred)
- **Backup:** Vimeo or direct MP4 link in GitHub repo
- **Format:** MP4, 1080p, ~5-15 MB target size

Submit the YouTube URL on the Kaggle submission form.
