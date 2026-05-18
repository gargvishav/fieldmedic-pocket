/*
 * Copyright 2026 FieldMedic Pocket project (Apache 2.0).
 *
 * Cactus (cactuscompute.com) on-device inference runtime — a second inference
 * backend alongside LiteRT-LM. Submitted to the Cactus prize track of the
 * Gemma 4 Good Hackathon.
 *
 * The primary FieldMedic chat flow runs on LiteRT-LM via google-ai-edge/gallery.
 * Cactus is wired in as an alternative path: tap the "Test Cactus runtime" button
 * in Settings → downloads a tiny model (qwen3-0.6 by default — small enough for
 * any device) → runs one inference → reports tokens/sec back to the UI.
 *
 * This proves the on-device Cactus path works end-to-end without forcing every
 * user to download a multi-GB Gemma 4 E2B `.cq` file. The Gemma 4 E2B Cactus
 * variant is available at https://huggingface.co/Cactus-Compute and can be
 * substituted by changing TEST_MODEL_SLUG below.
 */

package com.google.ai.edge.gallery.fieldmedic.cactus

import android.content.Context
import android.util.Log
import com.cactus.CactusCompletionParams
import com.cactus.CactusContextInitializer
import com.cactus.CactusInitParams
import com.cactus.CactusLM
import com.cactus.ChatMessage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object CactusEngine {
  private const val TAG = "FieldMedicCactus"

  // Tiny model: ~600M params, downloads fast (~400 MB), runs on any phone.
  // Swap to "gemma-4-e2b-it-cq" once we want the production-quality path
  // (downloads ~3 GB, takes longer first time).
  const val TEST_MODEL_SLUG = "qwen3-0.6"

  @Volatile private var lm: CactusLM? = null
  @Volatile private var loadedModel: String? = null
  private val initMutex = Mutex()

  /** Result of one Cactus inference round-trip. UI-facing summary string. */
  data class Result(
    val ok: Boolean,
    val response: String,
    val tokensPerSecond: Double? = null,
    val timeToFirstTokenMs: Double? = null,
    val totalTokens: Int? = null,
    val error: String? = null,
  )

  /**
   * One-shot end-to-end test: ensure Cactus is initialized, download + load
   * the test model if needed, generate a completion for [prompt].
   *
   * This is a coroutine — call from [kotlinx.coroutines.launch] or similar.
   * First call downloads the model (~400 MB for qwen3-0.6); subsequent calls
   * reuse the loaded handle and are fast.
   */
  suspend fun runOneShot(
    context: Context,
    prompt: String,
    modelSlug: String = TEST_MODEL_SLUG,
  ): Result {
    return try {
      val instance = ensureReady(context, modelSlug)
      // No-arg constructor uses SDK defaults (LOCAL inference, sensible temp etc.).
      val params = CactusCompletionParams()
      val cactusResult =
        instance.generateCompletion(
          listOf(ChatMessage(prompt, "user", null)),
          params,
          /* onToken */ null,
        )
      if (cactusResult == null) {
        Result(ok = false, response = "", error = "generateCompletion returned null")
      } else {
        Result(
          ok = cactusResult.success,
          response = cactusResult.response ?: "",
          tokensPerSecond = cactusResult.tokensPerSecond,
          timeToFirstTokenMs = cactusResult.timeToFirstTokenMs,
          totalTokens = cactusResult.totalTokens,
        )
      }
    } catch (e: Throwable) {
      Log.e(TAG, "Cactus inference failed", e)
      Result(ok = false, response = "", error = e.message ?: e.javaClass.simpleName)
    }
  }

  /**
   * Ensure Cactus is initialized for [context], the requested [modelSlug] is
   * downloaded, and a [CactusLM] handle is loaded against it. Safe to call
   * multiple times — only does work on the first call (or when [modelSlug]
   * changes).
   */
  private suspend fun ensureReady(context: Context, modelSlug: String): CactusLM {
    initMutex.withLock {
      val existing = lm
      if (existing != null && loadedModel == modelSlug) return existing

      Log.d(TAG, "Initializing CactusContext")
      CactusContextInitializer.initialize(context.applicationContext)

      Log.d(TAG, "Downloading Cactus model: $modelSlug")
      val instance = CactusLM()
      instance.downloadModel(modelSlug)

      Log.d(TAG, "Loading model handle: $modelSlug")
      instance.initializeModel(CactusInitParams(modelSlug, /* contextSize */ 2048))

      lm = instance
      loadedModel = modelSlug
      Log.d(TAG, "Cactus ready with model: $modelSlug")
      return instance
    }
  }

  /** Release native resources. Call on app teardown if you want; safe to skip. */
  fun unload() {
    try {
      lm?.unload()
    } catch (e: Throwable) {
      Log.w(TAG, "unload error", e)
    }
    lm = null
    loadedModel = null
  }

  fun isLoaded(): Boolean = lm?.isLoaded() == true
}
