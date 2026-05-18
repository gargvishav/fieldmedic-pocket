/*
 * Copyright 2026 FieldMedic Pocket project (Apache 2.0).
 *
 * Sherpa-ONNX-backed Piper TTS wrapper. Produces float32 PCM that we play via AudioTrack.
 * Used as the primary on-device TTS path; falls back silently to Android TextToSpeech if init fails.
 */

package com.google.ai.edge.gallery.fieldmedic.piper

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object PiperEngine {
  private const val TAG = "FieldMedicPiper"
  // Asset paths (inside APK assets dir).
  private const val MODEL_PATH = "piper/en_US-amy-low.onnx"
  private const val TOKENS_PATH = "piper/tokens.txt"
  private const val DATA_DIR = "piper/espeak-ng-data"

  @Volatile private var tts: OfflineTts? = null
  @Volatile private var sampleRate: Int = 22050
  private val initMutex = Mutex()
  private var currentTrack: AudioTrack? = null

  /** Lazily initialize. Returns true on success. Logs and returns false on failure. */
  suspend fun ensureReady(context: Context): Boolean {
    if (tts != null) return true
    return initMutex.withLock {
      if (tts != null) return@withLock true
      try {
        val vits =
          OfflineTtsVitsModelConfig(
            MODEL_PATH,
            "", // lexicon — not used for Piper voices
            TOKENS_PATH,
            DATA_DIR,
            "", // dictDir — not used
            0.667f, // noiseScale (Piper default)
            0.8f, // noiseScaleW (Piper default)
            1.0f, // lengthScale
          )
        val model =
          OfflineTtsModelConfig(
            vits,
            com.k2fsa.sherpa.onnx.OfflineTtsMatchaModelConfig(),
            com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig(),
            com.k2fsa.sherpa.onnx.OfflineTtsZipVoiceModelConfig(),
            com.k2fsa.sherpa.onnx.OfflineTtsKittenModelConfig(),
            com.k2fsa.sherpa.onnx.OfflineTtsPocketModelConfig(),
            com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig(),
            2, // numThreads
            false, // debug
            "cpu", // provider
          )
        val cfg = OfflineTtsConfig(model, "", "", 1, 0.2f)
        val instance = OfflineTts(context.assets, cfg)
        sampleRate = instance.sampleRate()
        tts = instance
        Log.d(TAG, "Piper ready, sampleRate=$sampleRate")
        true
      } catch (e: Throwable) {
        Log.w(TAG, "Piper init failed; will fall back to system TTS", e)
        false
      }
    }
  }

  /** Generate audio for the text and play it through AudioTrack. */
  suspend fun speak(context: Context, text: String) {
    if (text.isBlank()) return
    if (!ensureReady(context)) return
    val instance = tts ?: return
    try {
      // Stop any currently playing track.
      stop()
      val audio = instance.generate(text, 0, 1.0f)
      val samples = audio.samples
      if (samples.isEmpty()) return
      val pcm = ShortArray(samples.size)
      for (i in samples.indices) {
        val s = (samples[i] * 32767f).toInt().coerceIn(-32768, 32767)
        pcm[i] = s.toShort()
      }
      val bufSize =
        AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
          )
          .coerceAtLeast(pcm.size * 2)
      val track =
        AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_ASSISTANT)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(bufSize)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()
      track.write(pcm, 0, pcm.size)
      track.play()
      currentTrack = track
    } catch (e: Throwable) {
      Log.e(TAG, "Piper speak failed", e)
    }
  }

  fun stop() {
    try {
      currentTrack?.stop()
      currentTrack?.release()
    } catch (e: Throwable) {
      // ignore
    }
    currentTrack = null
  }

  fun isReady(): Boolean = tts != null
}
