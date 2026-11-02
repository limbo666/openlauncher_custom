package com.openlauncher.app.util

import android.media.AudioManager
import android.media.ToneGenerator
import com.openlauncher.app.data.TapSoundType

/**
 * Plays a short system-generated tap sound (tick / light beep) with no bundled
 * assets or audio permissions — ToneGenerator synthesizes the tone on the fly.
 *
 * [enabled] and [type] are kept up-to-date from settings by the app, so call
 * sites simply invoke [play] and it no-ops when the feature is off.
 */
object TapSound {

    @Volatile var enabled = false
    @Volatile var type = TapSoundType.TICK

    @Volatile private var generator: ToneGenerator? = null

    fun play() {
        if (!enabled) return
        val tone = when (type) {
            TapSoundType.TICK -> ToneGenerator.TONE_PROP_ACK
            TapSoundType.BEEP -> ToneGenerator.TONE_PROP_BEEP
        }
        val durationMs = if (type == TapSoundType.TICK) 60 else 120
        runCatching {
            if (generator == null) {
                generator = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
            }
            generator?.startTone(tone, durationMs)
        }
    }
}
