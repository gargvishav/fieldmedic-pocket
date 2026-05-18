/*
 * Copyright 2026 FieldMedic Pocket project (Apache 2.0).
 * Persistent record of one triage interaction.
 */

package com.google.ai.edge.gallery.fieldmedic.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "triage_events")
data class TriageEvent(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val timestamp: Long,
  val userInput: String,
  val function: String,
  val severity: String?,
  val headline: String?,
  val rawJson: String,
)
