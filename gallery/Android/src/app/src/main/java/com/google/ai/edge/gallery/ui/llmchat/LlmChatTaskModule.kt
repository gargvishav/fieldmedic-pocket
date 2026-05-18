/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.llmchat

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Mms
import com.google.ai.edge.litertlm.Contents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.R
import androidx.compose.ui.platform.LocalContext
import com.google.ai.edge.gallery.customtasks.common.CustomTask
import com.google.ai.edge.gallery.customtasks.common.CustomTaskDataForBuiltinTask
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Category
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.fieldmedic.FieldMedicPrefs
import com.google.ai.edge.gallery.fieldmedic.RamCheck
import com.google.ai.edge.gallery.ui.common.chat.FieldMedicOnboardingDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.ai.edge.gallery.runtime.runtimeHelper
import com.google.ai.edge.gallery.ui.theme.emptyStateContent
import com.google.ai.edge.gallery.ui.theme.emptyStateTitle
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

////////////////////////////////////////////////////////////////////////////////////////////////////
// FieldMedic Triage (replaces the gallery's default AI Chat for this fork).

private val FIELDMEDIC_SYSTEM_PROMPT =
  """
You are FieldMedic, an offline first-aid triage assistant. You help a non-medical user respond to an injury or emergency when professional help is delayed or unavailable.

You do NOT diagnose conditions. You do NOT prescribe medications. You triage URGENCY and give first-aid steps until trained help arrives.

# Your only job
For every input, decide ONE of these four actions and call the matching function:

1. report_triage     — when you can give actionable first-aid guidance
2. request_retake    — when a photo is unclear, too dark, or the injury isn't visible
3. escalate_emergency — when the situation is life-threatening NOW
4. refuse_out_of_scope — when the request isn't a first-aid scenario

# ALWAYS-ESCALATE — call escalate_emergency immediately, never report_triage
- Person is unconscious or not breathing normally
- Suspected stroke (face droop, slurred speech, weak arm)
- Severe allergic reaction (face/throat swelling, trouble breathing)
- Major bleeding from neck or groin, or pulsing/spurting blood from anywhere
- Seizure in progress
- Suspected spinal injury - don't move them
- Choking and unable to breathe or speak
- Snake bite, animal bite that punctures skin, scorpion sting, jellyfish sting in shock
- Electrical burn, lightning strike
- Burns covering more than the user's palm OR on face/hands/genitals/over a joint
- Eye injury with foreign object embedded or vision change
- Head injury in any child under 5
- Any injury with loss of consciousness, even briefly

# Severity rules (only after ruling out always-escalate)

RED — needs medical care within minutes to hours, but NOT immediately life-threatening
- Heavy bleeding that doesn't slow with 10 minutes of pressure
- Suspected broken bone or dislocation (visible deformity, joint at wrong angle)
- Deep cuts (visible fat, muscle, or bone)
- Animal bite that did NOT puncture (bruising / scrape only)
- ANY injury to a child under 5 (always at minimum RED) or pregnant person (bias one level higher)

YELLOW — needs care within 24 hours
- Burn smaller than palm, on safe areas (arm, leg, torso), no blisters yet
- CUT THAT NEEDS STITCHES — always YELLOW even if bleeding looks slow. A gaping cut, edges won't stay together, deeper than the skin layer, longer than 1cm = needs stitches.
- Sprain with swelling — YELLOW even if can't bear weight, UNLESS visible deformity or bone protrusion (then RED)
- HEAD IMPACT ON AN ADULT — always YELLOW minimum even if conscious and "feels fine"
- Wound that looks infected (red, warm, pus)
- Burn with blisters forming

GREEN — home care, watch and reassess
- Small superficial cuts and scrapes (clean breaks, edges meet, surface only)
- Tiny first-degree burns (red, no blisters, smaller than a thumbnail)
- Minor bruises with no swelling, no joint involvement
- Insect bites without allergic-reaction signs

# When uncertain, classify HIGHER severity. Never minimize. Never downgrade just because the helper sounds calm.

# Tone
- Speak directly. Short sentences. No medical jargon.
- Calm, not alarming.
- Address the HELPER, not the patient. Use "Tell them..." "Help them..." "Have them..."
- Lead with the single most important step.

# Hard rules
- NEVER name a medical condition with confidence. Say "looks like" or describe what you see.
- NEVER recommend medications by name or dose.
- NEVER reassure ("you'll be fine") - you cannot verify outcomes.
- ALWAYS include a "watch for" trigger that escalates to RED.

# Photo handling
- If the photo is blurry, too dark, too far away, or the injury isn't clearly in frame -> call request_retake with issue = "lighting" / "distance" / "focus" / "out_of_frame".
- If the photo shows something that isn't an injury (food, an object, an undamaged body part) -> call refuse_out_of_scope.
- Describe what you SEE plainly. Don't guess at what's underneath skin or clothing.
- A single clear photo can shift severity. Combine photo evidence with any text the user typed or spoke.

# Out of scope — call refuse_out_of_scope (NEVER escalate_emergency for these)
- Diagnosing diseases or naming medical conditions
- Medication advice (dose, interactions, prescriptions)
- MENTAL HEALTH CRISES, including self-harm — ALWAYS refuse_out_of_scope with category "mental_health". Redirect to a local crisis line (in India: iCall 9152987821 or Vandrevala 1860-2662-345). Do NOT escalate_emergency.
- ANY animal or pet injury (dog, cat, bird, livestock — even if they describe a wound or attach a photo). Category = "non_human".
- Dental or surgical advice
- Food safety
- General symptoms without injury

# Output format
Return EXACTLY one JSON object per input. No prose before or after. Schema must match the chosen function:

report_triage: {"function":"report_triage","severity":"RED|YELLOW|GREEN","headline":"...","immediate_steps":["..."],"do_not":["..."],"watch_for":["..."]}
escalate_emergency: {"function":"escalate_emergency","reason":"...","single_action":"...","while_waiting":["..."]}
request_retake: {"function":"request_retake","issue":"lighting|distance|focus|out_of_frame","instruction":"..."}
refuse_out_of_scope: {"function":"refuse_out_of_scope","category":"medication|diagnosis|mental_health|non_human|dental|food_safety|other","redirect":"..."}
""".trimIndent()

class LlmChatTask @Inject constructor() : CustomTask {
  override val task: Task =
    Task(
      id = BuiltInTaskId.LLM_CHAT,
      label = "FieldMedic",
      category = Category.LLM,
      icon = Icons.Outlined.MedicalServices,
      models = mutableListOf(),
      description = "Offline first-aid triage. Describe or photograph an injury and get an urgency rating with immediate steps. Not a doctor.",
      shortDescription = "Offline first-aid triage",
      docUrl = "",
      sourceCodeUrl = "",
      textInputPlaceHolderRes = R.string.text_input_placeholder_llm_chat,
      defaultSystemPrompt = FIELDMEDIC_SYSTEM_PROMPT,
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: (String) -> Unit,
  ) {
    model.runtimeHelper.initialize(
      context = context,
      model = model,
      supportImage = true,
      supportAudio = false,
      onDone = onDone,
      systemInstruction = Contents.of(FIELDMEDIC_SYSTEM_PROMPT),
      coroutineScope = coroutineScope,
    )
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    model.runtimeHelper.cleanUp(model = model, onDone = onDone)
  }

  @Composable
  override fun MainScreen(data: Any) {
    val myData = data as CustomTaskDataForBuiltinTask
    LlmChatScreen(
      modelManagerViewModel = myData.modelManagerViewModel,
      navigateUp = myData.onNavUp,
      showImagePicker = true,
      emptyStateComposable = { _, setInput ->
        val context = LocalContext.current
        val isLowRam = remember { RamCheck.isLowRamDevice(context) }
        val totalRamGb = remember { RamCheck.totalRamGb(context) }
        var showOnboarding by remember {
          mutableStateOf(!FieldMedicPrefs.hasSeenOnboarding(context))
        }
        if (showOnboarding) {
          FieldMedicOnboardingDialog(onFinished = { showOnboarding = false })
        }
        Box(modifier = Modifier.fillMaxSize()) {
          Column(
            modifier =
              Modifier.align(Alignment.Center).padding(horizontal = 24.dp).padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text("FieldMedic", style = emptyStateTitle)
            Text(
              "Offline first-aid triage. Snap a photo, speak, or type — and get an urgency rating with immediate steps.\n\nNot a doctor. Always consult a medical professional when possible.",
              style = emptyStateContent,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
            Text(
              "Try one of these:",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 8.dp),
            )
            val starterPrompts =
              listOf(
                "Patient unconscious, not breathing",
                "Someone is bleeding from the leg",
                "Burn from hot oil on hand",
                "Child fell off a bike, bump on head",
              )
            for (prompt in starterPrompts) {
              androidx.compose.material3.SuggestionChip(
                onClick = { setInput(prompt) },
                label = {
                  Text(
                    prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                  )
                },
                modifier = Modifier.padding(vertical = 2.dp),
              )
            }
            if (isLowRam) {
              Card(
                shape = RoundedCornerShape(12.dp),
                colors =
                  CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                  ),
                modifier = Modifier.padding(top = 12.dp),
              ) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Text(
                    "Low memory device detected (${"%.1f".format(totalRamGb)} GB)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                  )
                  Text(
                    "Gemma 4 E2B needs about 6 GB free RAM. On this phone it may be slow or run out of memory. Consider downloading the smaller Gemma 3 1B model from the model picker above for faster on-device inference.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                  )
                }
              }
            }
          }
        }
      },
    )
  }
}

@Module
@InstallIn(SingletonComponent::class) // Or another component that fits your scope
internal object LlmChatTaskModule {
  @Provides
  @IntoSet
  fun provideTask(): CustomTask {
    return LlmChatTask()
  }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// Ask image.

class LlmAskImageTask @Inject constructor() : CustomTask {
  override val task: Task =
    Task(
      id = BuiltInTaskId.LLM_ASK_IMAGE,
      label = "Ask Image",
      category = Category.LLM,
      icon = Icons.Outlined.Mms,
      models = mutableListOf(),
      description = "Ask questions about images with on-device large language models",
      shortDescription = "Ask questions about images",
      docUrl = "https://github.com/google-ai-edge/LiteRT-LM/blob/main/kotlin/README.md",
      sourceCodeUrl =
        "https://github.com/google-ai-edge/gallery/blob/main/Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatModelHelper.kt",
      textInputPlaceHolderRes = R.string.text_input_placeholder_llm_chat,
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: (String) -> Unit,
  ) {
    model.runtimeHelper.initialize(
      context = context,
      model = model,
      supportImage = true,
      supportAudio = false,
      onDone = onDone,
      coroutineScope = coroutineScope,
    )
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    model.runtimeHelper.cleanUp(model = model, onDone = onDone)
  }

  @Composable
  override fun MainScreen(data: Any) {
    val myData = data as CustomTaskDataForBuiltinTask
    LlmAskImageScreen(
      modelManagerViewModel = myData.modelManagerViewModel,
      navigateUp = myData.onNavUp,
    )
  }
}

@Module
@InstallIn(SingletonComponent::class) // Or another component that fits your scope
internal object LlmAskImageModule {
  @Provides
  @IntoSet
  fun provideTask(): CustomTask {
    return LlmAskImageTask()
  }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// Ask audio.

class LlmAskAudioTask @Inject constructor() : CustomTask {
  override val task: Task =
    Task(
      id = BuiltInTaskId.LLM_ASK_AUDIO,
      label = "Audio Scribe",
      category = Category.LLM,
      icon = Icons.Outlined.Mic,
      models = mutableListOf(),
      description =
        "Instantly transcribe and/or translate audio clips using on-device large language models",
      shortDescription = "Transcribe and translate audio",
      docUrl = "https://github.com/google-ai-edge/LiteRT-LM/blob/main/kotlin/README.md",
      sourceCodeUrl =
        "https://github.com/google-ai-edge/gallery/blob/main/Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatModelHelper.kt",
      textInputPlaceHolderRes = R.string.text_input_placeholder_llm_chat,
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: (String) -> Unit,
  ) {
    model.runtimeHelper.initialize(
      context = context,
      model = model,
      supportImage = false,
      supportAudio = true,
      onDone = onDone,
      coroutineScope = coroutineScope,
    )
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    model.runtimeHelper.cleanUp(model = model, onDone = onDone)
  }

  @Composable
  override fun MainScreen(data: Any) {
    val myData = data as CustomTaskDataForBuiltinTask
    LlmAskAudioScreen(
      modelManagerViewModel = myData.modelManagerViewModel,
      navigateUp = myData.onNavUp,
    )
  }
}

@Module
@InstallIn(SingletonComponent::class) // Or another component that fits your scope
internal object LlmAskAudioModule {
  @Provides
  @IntoSet
  fun provideTask(): CustomTask {
    return LlmAskAudioTask()
  }
}
