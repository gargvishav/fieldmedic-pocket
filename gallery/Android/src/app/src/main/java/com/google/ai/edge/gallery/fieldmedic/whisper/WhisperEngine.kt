/*
 * Copyright 2026 FieldMedic Pocket project (Apache 2.0).
 *
 * Thin wrapper around whisper.cpp's WhisperContext. Handles lazy init from APK assets,
 * thread-safe transcription, and language hinting.
 */

package com.google.ai.edge.gallery.fieldmedic.whisper

import android.content.Context
import android.util.Log
import com.whispercpp.whisper.WhisperContext
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object WhisperEngine {
  private const val TAG = "FieldMedicWhisper"
  private const val MODEL_ASSET_NAME = "ggml-tiny.bin"
  private const val MODEL_FILE_NAME = "ggml-tiny.bin"

  @Volatile private var ctx: WhisperContext? = null
  private val initMutex = Mutex()

  /** Copy the model from APK assets to internal storage on first use, then init context. */
  suspend fun ensureReady(context: Context) {
    if (ctx != null) return
    initMutex.withLock {
      if (ctx != null) return
      val modelFile = File(context.filesDir, MODEL_FILE_NAME)
      if (!modelFile.exists() || modelFile.length() < 1_000_000) {
        Log.d(TAG, "Copying model from assets to ${modelFile.absolutePath}")
        context.assets.open(MODEL_ASSET_NAME).use { input ->
          modelFile.outputStream().use { output -> input.copyTo(output) }
        }
      }
      Log.d(TAG, "Initializing WhisperContext from ${modelFile.absolutePath}")
      ctx = WhisperContext.createContextFromFile(modelFile.absolutePath)
      Log.d(TAG, "WhisperContext ready.")
    }
  }

  /**
   * Transcribe a chunk of mono PCM audio sampled at 16 kHz.
   * @param samples float32 PCM in -1.0..1.0
   * @return transcript text, or empty string if nothing was said
   */
  suspend fun transcribe(samples: FloatArray): String {
    val c = ctx ?: return ""
    if (samples.isEmpty()) return ""
    return try {
      // The bundled WhisperContext signature is (data, printTimestamp). We disable timestamps
      // because we want clean text to feed the chat input.
      c.transcribeData(samples, printTimestamp = false).trim()
    } catch (e: Exception) {
      Log.e(TAG, "Transcribe failed", e)
      ""
    }
  }

  fun isReady(): Boolean = ctx != null
}
