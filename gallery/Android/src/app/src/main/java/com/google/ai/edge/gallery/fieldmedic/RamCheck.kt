/*
 * Copyright 2026 FieldMedic Pocket project (Apache 2.0).
 *
 * Detect total device RAM. Used to recommend the smaller Gemma 3 1B model on low-end phones
 * (Gemma 4 E2B needs ~6 GB free; many <₹15K Android phones have only 4 GB total).
 */

package com.google.ai.edge.gallery.fieldmedic

import android.app.ActivityManager
import android.content.Context

object RamCheck {
  private const val LOW_RAM_THRESHOLD_BYTES = 6_000_000_000L // 6 GB

  fun totalRamBytes(context: Context): Long {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0L
    val mi = ActivityManager.MemoryInfo()
    am.getMemoryInfo(mi)
    return mi.totalMem
  }

  fun isLowRamDevice(context: Context): Boolean {
    val total = totalRamBytes(context)
    if (total <= 0L) return false
    return total < LOW_RAM_THRESHOLD_BYTES
  }

  fun totalRamGb(context: Context): Double {
    return totalRamBytes(context).toDouble() / 1_000_000_000.0
  }
}
