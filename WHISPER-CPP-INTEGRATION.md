# Whisper.cpp Integration Runbook

**Goal:** Replace `RecognizerIntent` (Google's STT) with whisper.cpp running fully on-device, so the airplane-mode demo doesn't depend on Google voice packs being installed.

**Estimated effort:** 1–2 focused days. **Do this fresh, not at midnight.**

**Why bother:** Cactus prize judges may test in airplane mode without ever installing voice packs. Today's `EXTRA_PREFER_OFFLINE = true` flag covers most cases; whisper.cpp covers all of them (the model file ships with the APK).

---

## Prereqs (have ready before starting)

- macOS with Android Studio + NDK installed (NDK r26+ recommended)
- CMake 3.22+ (Android Studio installs this)
- ~3 GB free build space
- Phone connected (we'll iterate fast)

---

## Step 1 — Get the source and a model

```
cd /Volumes/X10\ Pro/FieldMedic-Pocket/
git clone https://github.com/ggerganov/whisper.cpp.git
cd whisper.cpp
bash ./models/download-ggml-model.sh tiny.en   # ~75 MB, English only
# OR for multilingual:
bash ./models/download-ggml-model.sh tiny      # ~75 MB, all languages
# OR for tighter quantization:
bash ./models/download-ggml-model.sh tiny-q5_1 # ~30 MB
```

Move the chosen `.bin` to `gallery/Android/src/app/src/main/assets/whisper/ggml-tiny.bin`.

---

## Step 2 — Build the native library

whisper.cpp ships an Android example: `whisper.cpp/examples/whisper.android/`. Copy its `lib/` module into our gallery project as `gallery/Android/src/whisper/`.

Add to `gallery/Android/src/settings.gradle.kts`:
```kotlin
include(":whisper")
project(":whisper").projectDir = file("whisper")
```

Add to `gallery/Android/src/app/build.gradle.kts` dependencies:
```kotlin
implementation(project(":whisper"))
```

Run a clean build:
```
./gradlew :whisper:assembleRelease
```

You should see `libwhisper.so` produced under `whisper/build/intermediates/.../arm64-v8a/`. If not, fix CMake errors — usually missing NDK toolchain or wrong ABI filter.

---

## Step 3 — Kotlin wrapper

Create `gallery/Android/src/app/src/main/java/com/google/ai/edge/gallery/fieldmedic/whisper/WhisperEngine.kt`:

```kotlin
package com.google.ai.edge.gallery.fieldmedic.whisper

import android.content.Context
import com.whispercppdemo.whisper.WhisperContext   // class from whisper.cpp Android example

class WhisperEngine(private val context: Context) {
    private var ctx: WhisperContext? = null

    suspend fun init() {
        if (ctx != null) return
        // Copy the model from assets/whisper/ggml-tiny.bin to internal storage on first run.
        val modelFile = File(context.filesDir, "ggml-tiny.bin")
        if (!modelFile.exists()) {
            context.assets.open("whisper/ggml-tiny.bin").use { input ->
                modelFile.outputStream().use { input.copyTo(it) }
            }
        }
        ctx = WhisperContext.createContextFromFile(modelFile.absolutePath)
    }

    suspend fun transcribe(samples: FloatArray, lang: String = "en"): String {
        return ctx?.transcribeData(samples, lang = lang) ?: ""
    }

    fun close() {
        ctx?.release()
        ctx = null
    }
}
```

---

## Step 4 — Audio capture (16 kHz mono PCM)

Use Android's `AudioRecord` API. whisper.cpp expects float32 PCM at 16 kHz.

Pseudocode in a new `WhisperRecorder.kt`:

```kotlin
val sampleRate = 16000
val bufferSize = AudioRecord.getMinBufferSize(sampleRate, MONO, ENCODING_PCM_16BIT)
val recorder = AudioRecord(MIC, sampleRate, MONO, ENCODING_PCM_16BIT, bufferSize)
recorder.startRecording()
// ... read into ShortArray, normalize to FloatArray (-1.0..1.0), pass to engine.transcribe()
recorder.stop()
```

There's a usable `Recorder` class in `whisper.cpp/examples/whisper.android/lib/Recorder.kt` — copy and adapt.

---

## Step 5 — Wire into FieldMedic chat

Modify `MessageInputText.kt`'s mic button handler. Two paths:

**Option A — Replace RecognizerIntent fully.** Remove the SAF-style intent path; instead launch a custom audio-recording bottom sheet (the gallery's existing `AudioRecorderPanel.kt` is a good model), capture audio, send to Whisper, then `onValueChanged(transcription)`.

**Option B — Add a toggle.** Keep RecognizerIntent as default ("phone has voice pack"), add a "fully offline" mode that uses Whisper. Toggle in settings.

Option A is cleaner for the demo. Option B is safer for shipped users.

---

## Step 6 — Test airplane mode

```
adb shell svc wifi disable
adb shell svc data disable
adb shell settings put global airplane_mode_on 1
```

Open FieldMedic → tap mic → speak → should transcribe with no network. Time it (Whisper tiny on Snapdragon 888 ≈ 3–5 sec for 10 sec of audio).

---

## Known gotchas to budget for

| Gotcha | What you'll see | Fix |
|---|---|---|
| NDK toolchain mismatch | `cmake error: NDK at /path is invalid` | SDK Manager → install NDK r26 specifically |
| arm64 vs armeabi-v7a | Crash on `dlopen libwhisper.so` | Add `abiFilters("arm64-v8a")` to module gradle |
| Model load fails silently | `ctx == null` after `init()` | Check `assets/whisper/` is correctly bundled — `assets` not `assets/main/assets/` |
| Audio sounds garbled | Transcription is gibberish | Sample rate mismatch — must be 16 kHz mono float32 |
| OOM on first run | App killed | Use tiny-q5_1 model (~30 MB) instead of tiny.en |
| Long latency on Snapdragon 7-series | 15+ sec per query | Use tiny-q5_1; or threadcount tuning in whisper-context |

---

## Stretch — multilingual

`tiny.bin` (multilingual) supports Hindi, Bengali, Tamil, Marathi natively. Pass the language tag at transcribe time:

```kotlin
engine.transcribe(samples, lang = when (voiceLang) {
    "hi-IN" -> "hi"
    "bn-IN" -> "bn"
    "ta-IN" -> "ta"
    "mr-IN" -> "mr"
    else -> "en"
})
```

---

## Don't do this when starting fresh tomorrow

- Don't try to use the JVM-only `whisper-jni` library. It doesn't ship the Android `.so`.
- Don't expect a Maven artifact to exist that "just works". As of January 2026, whisper.cpp Android requires the source-build dance.
- Don't skip Step 2's clean build verification. If `libwhisper.so` isn't generated, no amount of Kotlin code will save you.
- Don't pick the `base` (~140 MB) or `small` (~470 MB) models for v1. tiny is plenty for first-aid triage queries.

---

## What's done in v0.1 (tonight) that helps

- Voice flow is already abstracted in `MessageInputText.kt`'s `handleClickVoiceToText`. Replacing it with Whisper means changing that one function — the rest of the chat UI stays.
- `EXTRA_PREFER_OFFLINE = true` is already on the existing recognizer, so users with installed voice packs get offline behavior today. Whisper extends this to *every* user.
- `voiceLang` state already cycles through 5 languages — Whisper just needs to map those tags to its language codes.

---

## Reference

- whisper.cpp main repo: https://github.com/ggerganov/whisper.cpp
- Android example dir: https://github.com/ggerganov/whisper.cpp/tree/master/examples/whisper.android
- Models page: https://huggingface.co/ggerganov/whisper.cpp
