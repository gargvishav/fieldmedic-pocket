/*
 * Copyright 2026 FieldMedic Pocket project (Apache 2.0).
 *
 * Handoff card UI — shows a clean, paramedic-friendly summary of the latest triage.
 * Shown as a Dialog over the chat when the user taps the Handoff button.
 */

package com.google.ai.edge.gallery.ui.common.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FieldMedicHandoffCardDialog(
  data: HandoffData,
  onDismiss: () -> Unit,
  onUpdateData: (HandoffData) -> Unit = {},
) {
  var showVitalsEntry by remember { mutableStateOf(false) }
  Dialog(
    onDismissRequest = onDismiss,
    properties =
      DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
  ) {
    Card(
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth().padding(24.dp),
    ) {
      LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              "FieldMedic Handoff",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
            data.severity?.let { SeverityBadge(it) }
          }
          Text(
            "Generated " + formatTime(data.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        // Headline / single_action / redirect / instruction.
        val headline =
          data.headline
            ?: data.singleAction
            ?: data.redirect
            ?: data.instruction
            ?: data.reason
        if (!headline.isNullOrBlank()) {
          item {
            Text(
              headline,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
        if (!data.reason.isNullOrBlank() && data.headline.isNullOrBlank()) {
          item { Text("Reason: ${data.reason}", style = MaterialTheme.typography.bodyMedium) }
        }

        sectionList("DO NOW", data.immediateSteps + data.whileWaiting)
        sectionList("DO NOT", data.doNotItems)
        sectionList("WATCH FOR (escalate if any of these)", data.watchFor)

        // Vitals section — shows entered values (if any) plus a button to add/edit.
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                Icons.Outlined.MonitorHeart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
              )
              Text(
                "VITALS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
            }
            TextButton(onClick = { showVitalsEntry = true }) {
              Text(if (data.hasAnyVitals()) "Edit" else "Add vitals")
            }
          }
        }
        if (data.hasAnyVitals()) {
          item {
            Column(modifier = Modifier.padding(start = 8.dp)) {
              data.heartRate?.let { Text("• Heart rate: $it bpm", style = MaterialTheme.typography.bodyMedium) }
              if (data.bpSystolic != null && data.bpDiastolic != null) {
                Text("• Blood pressure: ${data.bpSystolic}/${data.bpDiastolic} mmHg", style = MaterialTheme.typography.bodyMedium)
              }
              data.temperatureC?.let { Text("• Temperature: ${"%.1f".format(it)} °C", style = MaterialTheme.typography.bodyMedium) }
              data.spo2?.let { Text("• SpO₂: $it%", style = MaterialTheme.typography.bodyMedium) }
            }
          }
        }

        item {
          Text(
            "FieldMedic Pocket — offline first-aid triage. NOT a substitute for professional care. Show this to the responder when they arrive.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
          ) {
            TextButton(onClick = onDismiss) { Text("Close") }
          }
        }
      }
    }
  }
  if (showVitalsEntry) {
    VitalsEntryDialog(
      current = data,
      onDismiss = { showVitalsEntry = false },
      onSave = { updated ->
        onUpdateData(updated)
        showVitalsEntry = false
      },
    )
  }
}

private fun HandoffData.hasAnyVitals(): Boolean =
  heartRate != null || bpSystolic != null || bpDiastolic != null || temperatureC != null || spo2 != null

@Composable
private fun VitalsEntryDialog(
  current: HandoffData,
  onDismiss: () -> Unit,
  onSave: (HandoffData) -> Unit,
) {
  var hr by remember { mutableStateOf(current.heartRate?.toString().orEmpty()) }
  var sys by remember { mutableStateOf(current.bpSystolic?.toString().orEmpty()) }
  var dia by remember { mutableStateOf(current.bpDiastolic?.toString().orEmpty()) }
  var temp by remember { mutableStateOf(current.temperatureC?.toString().orEmpty()) }
  var spo by remember { mutableStateOf(current.spo2?.toString().orEmpty()) }
  Dialog(onDismissRequest = onDismiss) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(24.dp)) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text("Enter vitals", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
          "Leave blank if you don't have a value. Skip rather than guess.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = hr,
          onValueChange = { if (it.length <= 4) hr = it.filter(Char::isDigit) },
          label = { Text("Heart rate (bpm)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = sys,
            onValueChange = { if (it.length <= 4) sys = it.filter(Char::isDigit) },
            label = { Text("BP systolic") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = dia,
            onValueChange = { if (it.length <= 4) dia = it.filter(Char::isDigit) },
            label = { Text("BP diastolic") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
        }
        OutlinedTextField(
          value = temp,
          onValueChange = { temp = it.filter { c -> c.isDigit() || c == '.' } },
          label = { Text("Temperature (°C)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = spo,
          onValueChange = { if (it.length <= 3) spo = it.filter(Char::isDigit) },
          label = { Text("SpO₂ (%)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text("Cancel") }
          TextButton(
            onClick = {
              onSave(
                current.copy(
                  heartRate = hr.toIntOrNull(),
                  bpSystolic = sys.toIntOrNull(),
                  bpDiastolic = dia.toIntOrNull(),
                  temperatureC = temp.toFloatOrNull(),
                  spo2 = spo.toIntOrNull(),
                )
              )
            }
          ) {
            Text("Save")
          }
        }
      }
    }
  }
}

private fun androidx.compose.foundation.lazy.LazyListScope.sectionList(
  title: String,
  items: List<String>,
) {
  if (items.isEmpty()) return
  item {
    Text(
      title,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Bold,
    )
  }
  items(items.size) { idx ->
    Text(
      "• ${items[idx]}",
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.padding(start = 8.dp),
    )
  }
}

@Composable
private fun SeverityBadge(severity: String) {
  val (color, icon) =
    when (severity.uppercase()) {
      "RED" -> Color(0xFFD32F2F) to Icons.Filled.PriorityHigh
      "YELLOW" -> Color(0xFFF9A825) to Icons.Filled.Warning
      "GREEN" -> Color(0xFF2E7D32) to Icons.Outlined.CheckCircle
      else -> Color.Gray to Icons.Filled.Warning
    }
  Row(
    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    modifier =
      Modifier.clip(RoundedCornerShape(6.dp))
        .background(color)
        .padding(horizontal = 10.dp, vertical = 4.dp),
  ) {
    Icon(
      icon,
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.size(16.dp),
    )
    Text(
      severity.uppercase(),
      color = Color.White,
      fontWeight = FontWeight.Bold,
      style = MaterialTheme.typography.labelMedium,
    )
  }
}

private fun formatTime(ts: Long): String =
  SimpleDateFormat("HH:mm:ss, dd MMM yyyy", Locale.getDefault()).format(Date(ts))
