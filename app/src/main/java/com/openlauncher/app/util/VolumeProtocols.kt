package com.openlauncher.app.util

import android.content.Context
import android.content.Intent
import android.media.AudioManager

/**
 * A single way of commanding the head-unit volume. Each protocol is a probe: applying it
 * to a unit that does not listen is harmless (the broadcast has no receiver), while
 * applying it to the right unit changes the audible volume. That makes the calibration
 * wizard a safe "try them all and see which one sticks" detector.
 */
data class VolumeProtocol(
    val id: String,
    val name: String,
    val detail: String,
    val max: Int,
    val apply: (Context, Int) -> Unit
)

object VolumeProtocols {
    const val AUTO = "auto"

    /**
     * The well-known head-unit volume channels, ordered most-likely first:
     *  - stream     : standard Android STREAM_MUSIC (phones, emulators, Qualcomm units)
     *  - microntek  : Rockchip MTC units (PX3/PX5/PX6)
     *  - forfan     : Junsun / forfan units
     *  - fyt        : FYT / Joying units
     *  - hct        : HCT units
     *  - av_volume  : Rockchip audio-HAL parameter
     */
    val all: List<VolumeProtocol> = listOf(
        VolumeProtocol(
            id = "stream",
            name = "Standard Android (STREAM_MUSIC)",
            detail = "Normal Android audio stream. Typically works on emulators, phones and Qualcomm/Snapdragon units.",
            max = 100
        ) { ctx, v ->
            (ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0)
        },
        VolumeProtocol(
            id = "microntek",
            name = "Microntek / MTC",
            detail = "Rockchip MTC units — broadcast com.microntek.volumechange (extra volume, 0-30).",
            max = 30
        ) { ctx, v ->
            ctx.sendBroadcast(Intent("com.microntek.volumechange").putExtra("volume", v))
        },
        VolumeProtocol(
            id = "forfan",
            name = "Junsun / forfan",
            detail = "Junsun & forfan units — broadcast forfan.intent.action.VOLUMESET (extra volume).",
            max = 30
        ) { ctx, v ->
            ctx.sendBroadcast(Intent("forfan.intent.action.VOLUMESET").putExtra("volume", v))
        },
        VolumeProtocol(
            id = "fyt",
            name = "FYT / Joying / SYU",
            detail = "FYT-based units — broadcast com.syu.volumechange (extra volume).",
            max = 30
        ) { ctx, v ->
            ctx.sendBroadcast(Intent("com.syu.volumechange").putExtra("volume", v))
        },
        VolumeProtocol(
            id = "hct",
            name = "HCT",
            detail = "HCT units — broadcast com.hct.volumechange (extra volume). Experimental.",
            max = 30
        ) { ctx, v ->
            ctx.sendBroadcast(Intent("com.hct.volumechange").putExtra("volume", v))
        },
        VolumeProtocol(
            id = "av_volume",
            name = "Rockchip av_volume",
            detail = "Rockchip audio HAL parameter AudioManager.setParameters(\"av_volume=N\"). Experimental.",
            max = 30
        ) { ctx, v ->
            (ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.setParameters("av_volume=$v")
        },
        VolumeProtocol(
            id = "microntek_alt",
            name = "Microntek (alt key \"vol\")",
            detail = "Same broadcast with the volume in an extra named \"vol\". Experimental.",
            max = 30
        ) { ctx, v ->
            ctx.sendBroadcast(Intent("com.microntek.volumechange").putExtra("vol", v))
        },
        VolumeProtocol(
            id = "amp_volume",
            name = "Rockchip amp_volume",
            detail = "Audio HAL parameter AudioManager.setParameters(\"amp_volume=N\"). Experimental.",
            max = 30
        ) { ctx, v ->
            (ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.setParameters("amp_volume=$v")
        },
        VolumeProtocol(
            id = "mcu_volume",
            name = "MCU volume parameter",
            detail = "Audio HAL parameter AudioManager.setParameters(\"mcu_volume=N\"). Experimental.",
            max = 30
        ) { ctx, v ->
            (ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.setParameters("mcu_volume=$v")
        },
        VolumeProtocol(
            id = "microntek_volset",
            name = "Microntek VOLUME_SET (0-30)",
            detail = "com.microntek.VOLUME_SET with extra volume 0-30 — the documented MTC control action.",
            max = 30
        ) { ctx, v ->
            ctx.sendBroadcast(Intent("com.microntek.VOLUME_SET").putExtra("volume", v.coerceIn(0, 30)))
        },
        VolumeProtocol(
            id = "microntek_volset100",
            name = "Microntek VOLUME_SET (0-100)",
            detail = "Same action but the extra is scaled to 0-100 (older firmware scale).",
            max = 30
        ) { ctx, v ->
            val scaled = (v.coerceIn(0, 30) * 100) / 30
            ctx.sendBroadcast(Intent("com.microntek.VOLUME_SET").putExtra("volume", scaled))
        },
        VolumeProtocol(
            id = "microntek_control",
            name = "Microntek control.volume",
            detail = "com.microntek.control.volume with extra volume 0-30. Experimental.",
            max = 30
        ) { ctx, v ->
            ctx.sendBroadcast(Intent("com.microntek.control.volume").putExtra("volume", v.coerceIn(0, 30)))
        }
    )

    fun byId(id: String): VolumeProtocol = all.firstOrNull { it.id == id } ?: all.first()

    fun isKnown(id: String): Boolean = id == AUTO || all.any { it.id == id }
}
