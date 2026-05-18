/*
 * Copyright 2026 FieldMedic Pocket project (Apache 2.0).
 *
 * Settings dialog for FieldMedic — a single screen with a few toggles backed by SharedPreferences.
 */

package com.google.ai.edge.gallery.ui.common.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.ai.edge.gallery.fieldmedic.FieldMedicPrefs
import com.google.ai.edge.gallery.fieldmedic.cactus.CactusEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun FieldMedicSettingsDialog(onDismiss: () -> Unit) {
  val context = LocalContext.current
  remember { FieldMedicPrefs.init(context) }
  val usePiperState = FieldMedicPrefs.usePiperState()
  val autoSpeakState = FieldMedicPrefs.autoSpeakState()
  val usePiper by usePiperState
  val autoSpeak by autoSpeakState

  Dialog(onDismissRequest = onDismiss) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(24.dp)) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          "Settings",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        HorizontalDivider()

        SettingRow(
          title = "Read response aloud",
          subtitle = "Speak the triage automatically when Gemma finishes.",
          checked = autoSpeak,
          onCheckedChange = { FieldMedicPrefs.setAutoSpeak(context, it) },
        )
        HorizontalDivider()

        SettingRow(
          title = "Use offline neural voice (Piper)",
          subtitle =
            "Higher quality voice, fully offline, no Google Text-to-Speech needed. " +
              "Falls back to Android TTS for Hindi / Bengali / Tamil / Marathi.",
          checked = usePiper,
          onCheckedChange = { FieldMedicPrefs.setUsePiper(context, it) },
        )
        HorizontalDivider()

        // Cactus runtime test — proves our second on-device inference backend
        // works end-to-end. Submitted to the Cactus prize track. First tap
        // downloads ~400 MB; later taps are fast.
        val cactusScope = rememberCoroutineScope()
        var cactusStatus by remember { mutableStateOf("Idle") }
        var cactusBusy by remember { mutableStateOf(false) }
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
          Text("Cactus runtime (experimental)", style = MaterialTheme.typography.titleMedium)
          Text(
            "Second on-device inference path via cactuscompute.com. " +
              "First tap downloads a small test model (~400 MB); later taps are instant.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              "Status: $cactusStatus",
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Button(
              enabled = !cactusBusy,
              onClick = {
                cactusBusy = true
                cactusStatus = "Loading…"
                cactusScope.launch(Dispatchers.IO) {
                  val r =
                    CactusEngine.runOneShot(
                      context,
                      prompt = "Say hello in one short sentence.",
                    )
                  cactusStatus =
                    if (r.ok) {
                      "✅ ${r.response.take(80)} (${"%.1f".format(r.tokensPerSecond ?: 0.0)} tok/s)"
                    } else {
                      "❌ ${r.error ?: "unknown error"}"
                    }
                  cactusBusy = false
                }
              },
            ) {
              Text(if (cactusBusy) "Running…" else "Test")
            }
          }
        }
        HorizontalDivider()

        Text(
          "FieldMedic Pocket v0.1 — Gemma 4 E2B fine-tuned on 420 medical-triage examples, " +
            "with Whisper.cpp speech-to-text and Piper / Android TTS voice output. " +
            "Dual on-device inference: LiteRT-LM (primary) and Cactus (Cactus prize track). " +
            "Runs entirely on-device. Built for the Gemma 4 Good Hackathon.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text("Done") }
        }
      }
    }
  }
}

@Composable
private fun SettingRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}
