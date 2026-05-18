/*
 * Copyright 2026 FieldMedic Pocket project (Apache 2.0).
 *
 * Local-only Room database for FieldMedic. Stays on device. No network.
 */

package com.google.ai.edge.gallery.fieldmedic.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TriageEvent::class], version = 1, exportSchema = false)
abstract class FieldMedicDatabase : RoomDatabase() {
  abstract fun triageDao(): TriageDao

  companion object {
    @Volatile private var INSTANCE: FieldMedicDatabase? = null

    fun get(context: Context): FieldMedicDatabase =
      INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                FieldMedicDatabase::class.java,
                "fieldmedic.db",
              )
              .build()
              .also { INSTANCE = it }
        }
  }
}
