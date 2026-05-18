/*
 * Copyright 2026 FieldMedic Pocket project (Apache 2.0).
 *
 * Tiny SharedPreferences-backed settings store for FieldMedic-specific user preferences.
 */

package com.google.ai.edge.gallery.fieldmedic

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

object FieldMedicPrefs {
  private const val PREFS_NAME = "fieldmedic_prefs"
  private const val KEY_USE_PIPER = "use_piper_tts"
  private const val KEY_AUTO_SPEAK = "auto_speak_response"
  private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"

  private fun prefs(context: Context): SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  // Reactive state mirrors of the prefs so Composables recompose when changed.
  private val usePiperState: MutableState<Boolean> = mutableStateOf(true)
  private val autoSpeakState: MutableState<Boolean> = mutableStateOf(true)
  private val hasSeenOnboardingState: MutableState<Boolean> = mutableStateOf(false)
  private var initialized = false

  fun init(context: Context) {
    if (initialized) return
    val p = prefs(context)
    usePiperState.value = p.getBoolean(KEY_USE_PIPER, true)
    autoSpeakState.value = p.getBoolean(KEY_AUTO_SPEAK, true)
    hasSeenOnboardingState.value = p.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
    initialized = true
  }

  fun usePiper(context: Context): Boolean {
    init(context)
    return usePiperState.value
  }

  fun setUsePiper(context: Context, value: Boolean) {
    init(context)
    usePiperState.value = value
    prefs(context).edit().putBoolean(KEY_USE_PIPER, value).apply()
  }

  fun autoSpeak(context: Context): Boolean {
    init(context)
    return autoSpeakState.value
  }

  fun setAutoSpeak(context: Context, value: Boolean) {
    init(context)
    autoSpeakState.value = value
    prefs(context).edit().putBoolean(KEY_AUTO_SPEAK, value).apply()
  }

  fun hasSeenOnboarding(context: Context): Boolean {
    init(context)
    return hasSeenOnboardingState.value
  }

  fun markOnboardingSeen(context: Context) {
    init(context)
    hasSeenOnboardingState.value = true
    prefs(context).edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, true).apply()
  }

  fun usePiperState(): MutableState<Boolean> = usePiperState
  fun autoSpeakState(): MutableState<Boolean> = autoSpeakState
  fun hasSeenOnboardingState(): MutableState<Boolean> = hasSeenOnboardingState
}
