package com.openlauncher.app.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Three distinct alert tones for Telegram priority markers ($1 / $2 / $3).
 * Uses ToneGenerator (no audio assets needed) — plays on the notification
 * stream so it's audible over media without stopping playback.
 */
object AlertSound {

    fun play(level: Int) {
        if (level < 1) return
        runCatching {
            val stream = AudioManager.STREAM_NOTIFICATION
            val generator = ToneGenerator(stream, if (level >= 3) 95 else 85)
            val (tone, durationMs) = when (level) {
                3 -> ToneGenerator.TONE_SUP_INTERCEPT to 500            // urgent descending alert
                2 -> ToneGenerator.TONE_PROP_ACK to 300                 // two quick beeps
                else -> ToneGenerator.TONE_PROP_BEEP to 200             // single beep
            }
            generator.startTone(tone, durationMs)
            // Release after the tone has finished sounding.
            Handler(Looper.getMainLooper()).postDelayed({ runCatching { generator.release() } }, durationMs + 150L)
        }
    }
}
