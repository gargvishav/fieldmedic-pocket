/*
 * Copyright 2026 FieldMedic Pocket project (Apache 2.0).
 *
 * Captures 16 kHz mono PCM_16BIT and returns float32 samples normalized to -1..1
 * — exactly the format whisper.cpp wants.
 */

package com.google.ai.edge.gallery.fieldmedic.whisper

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class FieldMedicAudioRecorder {
  companion object {
    private const val TAG = "FMAudioRec"
    const val SAMPLE_RATE = 16000
  }

  private var record: AudioRecord? = null
  private val recording = AtomicBoolean(false)
  private val chunks = mutableListOf<ShortArray>()
  private val chunkLock = Any()

  @SuppressLint("MissingPermission") // RECORD_AUDIO is checked in the UI before we get here.
  fun start() {
    if (recording.get()) return
    val bufSize =
      AudioRecord.getMinBufferSize(
          SAMPLE_RATE,
          AudioFormat.CHANNEL_IN_MONO,
          AudioFormat.ENCODING_PCM_16BIT,
        )
        .coerceAtLeast(SAMPLE_RATE * 2) // at least 1 sec of buffer
    record =
      AudioRecord(
        MediaRecorder.AudioSource.MIC,
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufSize,
      )
    if (record?.state != AudioRecord.STATE_INITIALIZED) {
      Log.e(TAG, "AudioRecord failed to initialize")
      record?.release()
      record = null
      return
    }
    synchronized(chunkLock) { chunks.clear() }
    record?.startRecording()
    recording.set(true)
    val readBuf = ShortArray(bufSize / 2)
    thread(start = true, name = "FieldMedicRecorder") {
      while (recording.get()) {
        val r = record?.read(readBuf, 0, readBuf.size) ?: 0
        if (r > 0) synchronized(chunkLock) { chunks.add(readBuf.copyOf(r)) }
      }
    }
  }

  /** Stop recording and return the captured audio as float32 samples (-1..1). */
  fun stop(): FloatArray {
    if (!recording.get()) return FloatArray(0)
    recording.set(false)
    try {
      record?.stop()
    } catch (e: Exception) {
      Log.w(TAG, "AudioRecord.stop() error", e)
    }
    record?.release()
    record = null
    val total: Int
    val out: FloatArray
    synchronized(chunkLock) {
      total = chunks.sumOf { it.size }
      out = FloatArray(total)
      var pos = 0
      for (c in chunks) {
        for (s in c) {
          out[pos++] = s / 32768.0f
        }
      }
      chunks.clear()
    }
    Log.d(TAG, "Recorded ${total / SAMPLE_RATE.toFloat()}s of audio (${total} samples)")
    return out
  }

  fun isRecording(): Boolean = recording.get()
}
