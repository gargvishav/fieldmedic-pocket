/*
 * Copyright 2026 FieldMedic Pocket project (Apache 2.0).
 *
 * Locale-aware emergency dialer. Opens the phone dialer with the regional emergency number
 * pre-filled — the user still has to confirm by tapping call.
 */

package com.google.ai.edge.gallery.fieldmedic

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.TelephonyManager
import java.util.Locale

object EmergencyDialer {
  fun emergencyNumberFor(context: Context): String {
    // Prefer SIM country (most reliable for medical emergencies — you're physically there).
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    val country =
      (tm?.simCountryIso?.takeIf { it.isNotBlank() }
          ?: context.resources.configuration.locales[0].country)
        .uppercase(Locale.ROOT)
    return when (country) {
      "IN" -> "108"
      "US", "CA", "MX" -> "911"
      "GB", "IE" -> "999"
      "AU", "NZ" -> "000"
      "JP" -> "119"
      "KR" -> "119"
      "RU" -> "103"
      "BR" -> "192"
      // EU + much of the rest of the world have adopted 112.
      else -> "112"
    }
  }

  fun dial(context: Context) {
    val number = emergencyNumberFor(context)
    val intent =
      Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    runCatching { context.startActivity(intent) }
  }
}
