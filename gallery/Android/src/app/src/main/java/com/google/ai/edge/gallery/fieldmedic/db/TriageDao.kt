/*
 * Copyright 2026 FieldMedic Pocket project (Apache 2.0).
 */

package com.google.ai.edge.gallery.fieldmedic.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TriageDao {
  @Insert suspend fun insert(event: TriageEvent): Long

  @Query("SELECT * FROM triage_events ORDER BY timestamp DESC LIMIT 100")
  fun observeRecent(): Flow<List<TriageEvent>>

  @Query("SELECT COUNT(*) FROM triage_events") suspend fun count(): Int

  @Query("DELETE FROM triage_events") suspend fun clearAll()
}
