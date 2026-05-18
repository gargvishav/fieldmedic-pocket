/*
 * Copyright 2026 FieldMedic Pocket project (Apache 2.0).
 * History view: scrollable list of past triages stored in the local Room DB.
 */

package com.google.ai.edge.gallery.ui.common.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.ai.edge.gallery.fieldmedic.db.FieldMedicDatabase
import com.google.ai.edge.gallery.fieldmedic.db.TriageEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.flowOf

@Composable
fun FieldMedicHistoryDialog(onDismiss: () -> Unit) {
  val context = LocalContext.current
  val dao = remember { FieldMedicDatabase.get(context).triageDao() }
  val events by remember { dao.observeRecent() }.collectAsState(initial = emptyList())

  Dialog(
    onDismissRequest = onDismiss,
    properties =
      DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
  ) {
    Card(
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth().padding(24.dp),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            "Triage history",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          Text(
            "${events.size} event${if (events.size == 1) "" else "s"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        HorizontalDivider()

        if (events.isEmpty()) {
          Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
              "No triages yet. Send your first prompt to record one.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(events, key = { it.id }) { event -> TriageHistoryRow(event) }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onDismiss) { Text("Close") }
        }
      }
    }
  }
}

@Composable
private fun TriageHistoryRow(event: TriageEvent) {
  Card(
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth(),
    colors =
      androidx.compose.material3.CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
      ),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          formatTimestamp(event.timestamp),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SeverityChip(event.severity, event.function)
      }
      val title =
        event.headline ?: event.userInput.take(80) +
          if (event.userInput.length > 80) "..." else ""
      Text(
        title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp),
      )
      if (!event.headline.isNullOrBlank() && event.userInput.isNotBlank()) {
        Text(
          "Asked: ${event.userInput.take(120)}${if (event.userInput.length > 120) "..." else ""}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 2.dp),
        )
      }
    }
  }
}

@Composable
private fun SeverityChip(severity: String?, function: String) {
  data class ChipStyle(val label: String, val color: Color, val icon: androidx.compose.ui.graphics.vector.ImageVector)
  val style =
    when {
      function == "escalate_emergency" || function == "escalation_emergency" ->
        ChipStyle("EMERGENCY", Color(0xFFB71C1C), Icons.Outlined.LocalHospital)
      function == "refuse_out_of_scope" ->
        ChipStyle("OUT OF SCOPE", Color.Gray, Icons.Outlined.Block)
      function == "request_retake" ->
        ChipStyle("RETAKE", Color(0xFF1976D2), Icons.Outlined.CameraAlt)
      severity?.uppercase() == "RED" ->
        ChipStyle("RED", Color(0xFFD32F2F), Icons.Filled.PriorityHigh)
      severity?.uppercase() == "YELLOW" ->
        ChipStyle("YELLOW", Color(0xFFF9A825), Icons.Filled.Warning)
      severity?.uppercase() == "GREEN" ->
        ChipStyle("GREEN", Color(0xFF2E7D32), Icons.Outlined.CheckCircle)
      else -> ChipStyle("—", Color.Gray, Icons.Filled.Warning)
    }
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    modifier =
      Modifier.clip(RoundedCornerShape(6.dp))
        .background(style.color)
        .padding(horizontal = 6.dp, vertical = 2.dp),
  ) {
    Icon(
      style.icon,
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.size(12.dp),
    )
    Text(
      style.label,
      color = Color.White,
      fontWeight = FontWeight.Bold,
      style = MaterialTheme.typography.labelSmall,
    )
  }
}

private fun formatTimestamp(ts: Long): String =
  SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault()).format(Date(ts))
