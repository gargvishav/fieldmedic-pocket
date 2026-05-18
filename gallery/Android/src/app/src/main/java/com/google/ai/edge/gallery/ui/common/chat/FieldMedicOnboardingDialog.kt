/*
 * Copyright 2026 FieldMedic Pocket project (Apache 2.0).
 *
 * First-launch onboarding: 3 swipeable screens that introduce the app.
 * Shown once; "Get started" marks the SharedPrefs flag so it never reappears.
 */

package com.google.ai.edge.gallery.ui.common.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.ai.edge.gallery.fieldmedic.FieldMedicPrefs
import kotlinx.coroutines.launch

private data class OnboardingPage(
  val icon: ImageVector,
  val accent: Color,
  val title: String,
  val body: String,
)

private val ONBOARDING_PAGES =
  listOf(
    OnboardingPage(
      Icons.Outlined.Mic,
      Color(0xFFD32F2F),
      "Speak in panic mode",
      "Tap the mic and just describe what happened. Voice input works in 5 languages — English, Hindi, Bengali, Tamil, Marathi.",
    ),
    OnboardingPage(
      Icons.Outlined.PhotoCamera,
      Color(0xFFF9A825),
      "Or snap an injury photo",
      "Tap + → Take a picture. FieldMedic looks at the photo and your description together to triage urgency: red, yellow, or green.",
    ),
    OnboardingPage(
      Icons.Outlined.WifiOff,
      Color(0xFF2E7D32),
      "Works fully offline",
      "Tap the wifi-off mic for offline-only voice input via Whisper.cpp. Even in airplane mode, FieldMedic gives first-aid steps. No internet required.",
    ),
  )

@Composable
fun FieldMedicOnboardingDialog(onFinished: () -> Unit) {
  val context = LocalContext.current
  val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGES.size })
  val scope = rememberCoroutineScope()

  Dialog(
    onDismissRequest = { /* require explicit Get Started or Skip */ },
    properties =
      DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
      ),
  ) {
    Card(
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth().padding(24.dp),
    ) {
      Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Skip in top-right.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(
            onClick = {
              FieldMedicPrefs.markOnboardingSeen(context)
              onFinished()
            }
          ) {
            Text("Skip")
          }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
          val p = ONBOARDING_PAGES[page]
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Box(
              modifier = Modifier.size(96.dp).clip(CircleShape).background(p.accent),
              contentAlignment = Alignment.Center,
            ) {
              Icon(p.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Text(
              p.title,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center,
            )
            Text(
              p.body,
              style = MaterialTheme.typography.bodyMedium,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Spacer(Modifier.height(8.dp))
        // Page indicator dots.
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          for (i in 0 until ONBOARDING_PAGES.size) {
            val isCurrent = pagerState.currentPage == i
            Box(
              modifier =
                Modifier.size(if (isCurrent) 10.dp else 8.dp)
                  .clip(CircleShape)
                  .background(
                    if (isCurrent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                  )
            )
          }
        }

        Spacer(Modifier.height(16.dp))

        Button(
          onClick = {
            if (pagerState.currentPage == ONBOARDING_PAGES.size - 1) {
              FieldMedicPrefs.markOnboardingSeen(context)
              onFinished()
            } else {
              scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            if (pagerState.currentPage == ONBOARDING_PAGES.size - 1) "Get started" else "Next"
          )
        }
      }
    }
  }
}
