/*
 * Copyright 2026 FieldMedic Pocket project (Apache 2.0).
 *
 * Text-to-speech helper for FieldMedic. Auto-speaks the human-readable parts
 * of Gemma's JSON triage output when an assistant message completes.
 */

package com.google.ai.edge.gallery.ui.common.chat

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.ai.edge.gallery.fieldmedic.FieldMedicPrefs
import com.google.ai.edge.gallery.fieldmedic.piper.PiperEngine
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val TAG = "FieldMedicTts"

class FieldMedicTtsState(context: Context) {
  private val appContext = context.applicationContext
  private var tts: TextToSpeech? = null
  private var ready = false
  @Volatile var enabled: Boolean = true
  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  init {
    tts =
      TextToSpeech(appContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
          val res = tts?.setLanguage(Locale("en", "IN"))
          if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.US)
          }
          tts?.setSpeechRate(1.0f)
        } else {
          Log.w(TAG, "TextToSpeech init failed (status=$status)")
        }
      }
  }

  fun speak(text: String) {
    if (!enabled || text.isBlank()) return
    if (!FieldMedicPrefs.autoSpeak(appContext)) return
    val hasIndicScript =
      text.any { it in 'ঀ'..'৿' || it in '஀'..'௿' || it in 'ऀ'..'ॿ' }
    if (hasIndicScript || !FieldMedicPrefs.usePiper(appContext)) {
      // Indic script -> Android TTS. Or user has disabled Piper in settings.
      speakViaAndroidTts(text)
      return
    }
    // English + Piper enabled: try Piper first. Fall back to Android TTS on init failure.
    scope.launch {
      val piperOk = PiperEngine.ensureReady(appContext)
      if (piperOk) {
        PiperEngine.speak(appContext, text)
      } else {
        speakViaAndroidTts(text)
      }
    }
  }

  private fun speakViaAndroidTts(text: String) {
    if (!ready) return
    val locale =
      when {
        text.any { it in 'ঀ'..'৿' } -> Locale("bn", "IN")
        text.any { it in '஀'..'௿' } -> Locale("ta", "IN")
        text.any { it in 'ऀ'..'ॿ' } -> Locale("hi", "IN")
        else -> Locale("en", "IN")
      }
    val res = tts?.setLanguage(locale)
    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
      Log.w(TAG, "Voice for $locale not installed; falling back to en-IN")
      tts?.setLanguage(Locale("en", "IN"))
    }
    tts?.stop()
    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "fieldmedic_${System.currentTimeMillis()}")
  }

  fun setLanguageTag(langTag: String) {
    if (!ready) return
    val locale =
      when (langTag) {
        "hi-IN" -> Locale("hi", "IN")
        "en-IN" -> Locale("en", "IN")
        else -> Locale.US
      }
    val res = tts?.setLanguage(locale)
    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
      Log.w(TAG, "TTS language $langTag not installed; falling back to en-IN")
      tts?.setLanguage(Locale("en", "IN"))
    }
  }

  fun shutdown() {
    try {
      tts?.stop()
      tts?.shutdown()
      PiperEngine.stop()
      scope.cancel()
    } catch (e: Exception) {
      Log.w(TAG, "TTS shutdown error", e)
    }
    tts = null
  }
}

@Composable
fun rememberFieldMedicTts(): FieldMedicTtsState {
  val context = LocalContext.current
  val state = remember { FieldMedicTtsState(context) }
  DisposableEffect(Unit) { onDispose { state.shutdown() } }
  return state
}

/** Structured handoff card data extracted from Gemma's JSON triage response. */
data class HandoffData(
  val timestamp: Long,
  val function: String,
  val severity: String?,
  val headline: String?,
  val reason: String?,
  val singleAction: String?,
  val redirect: String?,
  val instruction: String?,
  val immediateSteps: List<String>,
  val doNotItems: List<String>,
  val watchFor: List<String>,
  val whileWaiting: List<String>,
  // Optional vitals — entered manually from the handoff card UI when the helper has them.
  val heartRate: Int? = null, // beats per minute
  val bpSystolic: Int? = null, // mmHg
  val bpDiastolic: Int? = null, // mmHg
  val temperatureC: Float? = null,
  val spo2: Int? = null, // % oxygen saturation
)

private fun org.json.JSONArray?.toStringList(): List<String> {
  if (this == null) return emptyList()
  val out = mutableListOf<String>()
  for (i in 0 until length()) {
    val s = optString(i)
    if (s.isNotBlank()) out.add(s)
  }
  return out
}

/** Parse Gemma's JSON triage response into HandoffData. Null if not recognizable JSON. */
fun extractHandoffData(raw: String): HandoffData? {
  val cleaned = extractJsonObject(raw) ?: return null
  return try {
    val json = JSONObject(cleaned)
    val func = canonicalFunction(json.optString("function")) ?: return null
    HandoffData(
      timestamp = System.currentTimeMillis(),
      function = func,
      severity = json.optString("severity").ifBlank { null },
      headline = json.optString("headline").ifBlank { null },
      reason = json.optString("reason").ifBlank { null },
      singleAction = json.optString("single_action").ifBlank { null },
      redirect = json.optString("redirect").ifBlank { null },
      instruction = json.optString("instruction").ifBlank { null },
      immediateSteps = json.optJSONArray("immediate_steps").toStringList(),
      doNotItems = json.optJSONArray("do_not").toStringList(),
      watchFor = json.optJSONArray("watch_for").toStringList(),
      whileWaiting = json.optJSONArray("while_waiting").toStringList(),
    )
  } catch (e: Exception) {
    null
  }
}

private fun hasDevanagari(s: String?): Boolean = s?.any { it in 'ऀ'..'ॿ' } == true

/**
 * Pulls the first balanced JSON object out of a raw string. Tolerates code-fence wrappers,
 * leading/trailing prose, partial streams, and trailing commas.
 */
private fun extractJsonObject(raw: String): String? {
  if (raw.isBlank()) return null
  val cleaned =
    raw
      .trim()
      .removePrefix("```json")
      .removePrefix("```")
      .removeSuffix("```")
      .trim()
  val start = cleaned.indexOf('{')
  if (start == -1) return null
  // Walk braces to find the matching close — handles trailing prose past the JSON.
  var depth = 0
  var inString = false
  var escape = false
  for (i in start until cleaned.length) {
    val c = cleaned[i]
    when {
      escape -> escape = false
      inString && c == '\\' -> escape = true
      c == '"' -> inString = !inString
      inString -> {}
      c == '{' -> depth++
      c == '}' -> {
        depth--
        if (depth == 0) {
          // Strip trailing commas before } or ] (a common Gemma drift).
          return cleaned.substring(start, i + 1).replace(Regex(",\\s*([}\\]])"), "$1")
        }
      }
    }
  }
  return null
}

/**
 * Normalize the model's `function` field to one of our 4 canonical names. Accepts
 * variations like "escalation_emergency", "escalateEmergency", " ESCALATE-EMERGENCY ".
 */
private fun canonicalFunction(raw: String?): String? {
  if (raw.isNullOrBlank()) return null
  val key = raw.trim().lowercase().replace(Regex("[^a-z]"), "")
  return when (key) {
    "reporttriage", "triage", "report" -> "report_triage"
    "escalateemergency", "escalationemergency", "escalate", "emergency", "emergencyescalate" ->
      "escalate_emergency"
    "refuseoutofscope", "outofscope", "refuse" -> "refuse_out_of_scope"
    "requestretake", "retake" -> "request_retake"
    else -> null
  }
}

private fun severityHindi(severity: String): String =
  when (severity.uppercase()) {
    "GREEN" -> "कम गंभीर।"
    "YELLOW" -> "मध्यम गंभीर।"
    "RED" -> "बहुत गंभीर।"
    else -> ""
  }

/**
 * Parse Gemma's JSON triage response and return a short, panic-friendly string suitable for TTS.
 * Returns null if the message isn't recognizable JSON or has no extractable speakable content.
 *
 * Auto-detects whether the response is in Hindi (Devanagari script). If so, prefixes with Hindi
 * severity terms instead of English so the TTS engine picks the Hindi voice.
 */
fun extractSpeakableText(raw: String): String? {
  val cleaned = extractJsonObject(raw) ?: return null
  return try {
    val json = JSONObject(cleaned)
    when (val func = canonicalFunction(json.optString("function"))) {
      "report_triage" -> {
        val severity = json.optString("severity")
        val headline = json.optString("headline")
        val immediateSteps = json.optJSONArray("immediate_steps").toStringList()
        val doNot = json.optJSONArray("do_not").toStringList()
        val watchFor = json.optJSONArray("watch_for").toStringList()
        val isHindi =
          hasDevanagari(headline) ||
            immediateSteps.any { hasDevanagari(it) } ||
            doNot.any { hasDevanagari(it) }
        val parts = mutableListOf<String>()
        if (isHindi) {
          parts += severityHindi(severity)
          if (headline.isNotBlank()) parts += headline.trimEnd('.', '।') + "।"
          if (immediateSteps.isNotEmpty()) {
            parts += "तुरंत करें।"
            parts += immediateSteps
          }
          if (doNot.isNotEmpty()) {
            parts += "ये न करें।"
            parts += doNot
          }
          if (watchFor.isNotEmpty()) {
            parts += "इन पर नज़र रखें।"
            parts += watchFor
          }
        } else {
          parts += "Severity $severity."
          if (headline.isNotBlank()) parts += headline.trimEnd('.') + "."
          if (immediateSteps.isNotEmpty()) {
            parts += "Do this now."
            parts += immediateSteps
          }
          if (doNot.isNotEmpty()) {
            parts += "Do not."
            parts += doNot
          }
          if (watchFor.isNotEmpty()) {
            parts += "Watch for."
            parts += watchFor
          }
        }
        parts.filter { it.isNotBlank() }.joinToString(" ")
      }
      "escalate_emergency" -> {
        val action = json.optString("single_action")
        val reason = json.optString("reason")
        val whileWaiting = json.optJSONArray("while_waiting").toStringList()
        val isHindi =
          hasDevanagari(action) ||
            hasDevanagari(reason) ||
            whileWaiting.any { hasDevanagari(it) }
        val parts = mutableListOf<String>()
        if (isHindi) {
          parts += "आपातकाल।"
          if (reason.isNotBlank()) parts += reason
          if (action.isNotBlank()) parts += action
          if (whileWaiting.isNotEmpty()) {
            parts += "मदद आने तक।"
            parts += whileWaiting
          }
        } else {
          parts += "Emergency."
          if (reason.isNotBlank()) parts += reason
          if (action.isNotBlank()) parts += action
          if (whileWaiting.isNotEmpty()) {
            parts += "While waiting."
            parts += whileWaiting
          }
        }
        parts.filter { it.isNotBlank() }.joinToString(" ")
      }
      "refuse_out_of_scope" -> json.optString("redirect").ifBlank { null }
      "request_retake" -> json.optString("instruction").ifBlank { null }
      else -> {
        Log.d(TAG, "Unrecognized function in JSON: $func")
        null
      }
    }
  } catch (e: Exception) {
    Log.d(TAG, "Not valid JSON, skipping TTS: ${e.message}")
    null
  }
}
