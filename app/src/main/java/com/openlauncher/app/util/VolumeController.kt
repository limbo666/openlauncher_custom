package com.openlauncher.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-wide head-unit volume controller.
 *
 * Head-unit audio is frequently MCU-controlled, where STREAM_MUSIC is only a mirror
 * (writing it moves a number but never changes what you hear). The active channel is
 * chosen from [VolumeProtocols]:
 *
 *   "auto"      → Microntek broadcast if com.microntek.* is installed, else STREAM_MUSIC
 *   any id      → that exact protocol (set by the calibration wizard)
 *
 * The level range is detected per-device (STREAM_MUSIC max for the stream protocol,
 * the protocol's own max otherwise) so the widget never assumes a fixed scale.
 */
object VolumeController {
    private val _level = MutableStateFlow<Int?>(null)
    val level: StateFlow<Int?> = _level

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted

    private val _max = MutableStateFlow(15)
    val max: StateFlow<Int> = _max

    private val _vendor = MutableStateFlow("stream")
    val vendor: StateFlow<String> = _vendor

    private val _isMicrontek = MutableStateFlow(false)
    val isMicrontek: StateFlow<Boolean> = _isMicrontek

    @Volatile private var protocolId: String = VolumeProtocols.AUTO
    @Volatile private var preMute = -1
    @Volatile private var initialized = false
    @Volatile private var appCtx: Context? = null
    @Volatile private var lastCommandAt = 0L

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val app = context.applicationContext
            appCtx = app
            _isMicrontek.value = runCatching {
                app.packageManager.getInstalledApplications(0).any { it.packageName.startsWith("com.microntek") }
            }.getOrDefault(false)

            refreshResolved()
            val am = app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val cur = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
            _level.value = cur.coerceIn(0, _max.value)
            _muted.value = _level.value == 0

            registerReceivers(app)
            initialized = true
            FileLog.log(
                "Volume",
                "controller init protocol=$protocolId resolved=${_vendor.value} level=${_level.value} max=${_max.value} microntek=${_isMicrontek.value}"
            )
            runCatching {
                FileLog.log("Volume", "cfg_maxvolume query=${am?.getParameters("cfg_maxvolume=")}")
            }
        }
    }

    /** Called when the user picks a protocol in the calibration wizard (or settings load). */
    fun setProtocol(id: String) {
        if (protocolId == id) return
        protocolId = id
        if (initialized) refreshResolved()
        FileLog.log("Volume", "setProtocol $id resolved=${_vendor.value} max=${_max.value}")
    }

    private fun refreshResolved() {
        val app = appCtx ?: return
        val am = app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val streamMax = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val id = if (protocolId == VolumeProtocols.AUTO) {
            if (_isMicrontek.value) "microntek_volset" else "stream"
        } else protocolId
        val proto = VolumeProtocols.byId(id)
        val effectiveMax = if (proto.id == "stream") streamMax.coerceAtLeast(1) else proto.max
        _vendor.value = proto.id
        _max.value = effectiveMax
        _level.value = (_level.value ?: 0).coerceIn(0, effectiveMax)
        _muted.value = _level.value == 0
    }

    fun set(context: Context, target: Int) {
        val app = appCtx ?: context.applicationContext
        val am = app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val streamMax = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val id = if (protocolId == VolumeProtocols.AUTO) {
            if (_isMicrontek.value) "microntek_volset" else "stream"
        } else protocolId
        val proto = VolumeProtocols.byId(id)
        val effectiveMax = if (proto.id == "stream") streamMax.coerceAtLeast(1) else proto.max
        val v = target.coerceIn(0, effectiveMax)
        _level.value = v
        _muted.value = v == 0
        if (v > 0) preMute = v
        val wire = if (proto.id == "microntek_volset100") (v * 100) / 30 else v
        runCatching { proto.apply(context, v) }
        lastCommandAt = android.os.SystemClock.uptimeMillis()
        FileLog.log("Volume", "set protocol=${proto.id} v=$v wire=$wire max=$effectiveMax")
    }

    fun adjust(context: Context, direction: Int) {
        val cur = _level.value ?: 0
        set(context, cur + direction)
    }

    fun toggleMute(context: Context) {
        val cur = _level.value ?: 0
        if (cur > 0) {
            preMute = cur
            set(context, 0)
            FileLog.log("Mute", "mute (was $cur)")
        } else {
            val restore = if (preMute > 0) preMute else (_max.value / 2).coerceAtLeast(1)
            set(context, restore)
            FileLog.log("Mute", "unmute -> $restore")
        }
    }

    /** Send a level through a protocol WITHOUT touching tracked state — used by the wizard. */
    fun applyProtocolForTest(context: Context, protocol: VolumeProtocol, level: Int) {
        runCatching { protocol.apply(context, level.coerceIn(0, protocol.max)) }
        FileLog.log("Volume", "wizard test protocol=${protocol.id} level=$level")
    }

    private fun registerReceivers(app: Context) {
        // Keep the on-screen value in sync with the system stream on standard devices.
        runCatching {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    if (intent?.action != "android.media.VOLUME_CHANGED_ACTION") return
                    if (!isStreamActive()) return
                    val am = c?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    val v = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: return
                    _level.value = v.coerceIn(0, _max.value)
                    _muted.value = _level.value == 0
                }
            }
            val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                app.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                app.registerReceiver(receiver, filter)
            }
        }
        // Keep the on-screen value in sync with the MCU's OUTBOUND volume notification.
        // MTCManager broadcasts VOLUME_CHANGED (uppercase) when the physical knob / SWC
        // changes the real volume; some firmwares use the lowercase action instead.
        runCatching {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val action = intent?.action ?: return
                    if (action != "com.microntek.VOLUME_CHANGED" && action != "com.microntek.volumechange") return
                    if (android.os.SystemClock.uptimeMillis() - lastCommandAt < 500) return
                    val wire = intent.getIntExtra("volume", -1)
                    if (wire in 0..100) {
                        val level = wireToLevel(wire)
                        _level.value = level
                        _muted.value = level == 0
                        if (level > 0) preMute = level
                        FileLog.log("Volume", "mcu volume changed wire=$wire level=$level max=${_max.value}")
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction("com.microntek.VOLUME_CHANGED")
                addAction("com.microntek.volumechange")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                app.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                app.registerReceiver(receiver, filter)
            }
        }
        // Secondary: some firmwares write the level to Settings.System on knob turn.
        runCatching {
            val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    if (android.os.SystemClock.uptimeMillis() - lastCommandAt < 500) return
                    val raw = android.provider.Settings.System.getString(app.contentResolver, "av_volume")
                    val wire = raw?.toIntOrNull() ?: return
                    if (wire in 0..100) {
                        val level = wireToLevel(wire)
                        _level.value = level
                        _muted.value = level == 0
                        if (level > 0) preMute = level
                        FileLog.log("Volume", "settings av_volume changed wire=$wire level=$level")
                    }
                }
            }
            app.contentResolver.registerContentObserver(
                android.provider.Settings.System.getUriFor("av_volume"), false, observer
            )
        }
    }

    private fun isStreamActive(): Boolean =
        protocolId == "stream" || (protocolId == VolumeProtocols.AUTO && !_isMicrontek.value)

    /** The MCU reports its native scale (0-30 on MTC units), which already matches [_max]. */
    private fun wireToLevel(wire: Int): Int = wire.coerceIn(0, _max.value)

    /** One-shot dump of everything the app can see about the unit's audio layer. */
    fun collectAudioInfo(context: Context): String {
        val sb = StringBuilder()
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        sb.appendLine("AUDIO INFO")
        sb.appendLine("  Build: ${Build.MANUFACTURER}/${Build.MODEL}/${Build.DEVICE}/${Build.HARDWARE}")
        sb.appendLine("  Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        val streams = listOf(
            AudioManager.STREAM_VOICE_CALL to "VOICE_CALL",
            AudioManager.STREAM_SYSTEM to "SYSTEM",
            AudioManager.STREAM_RING to "RING",
            AudioManager.STREAM_MUSIC to "MUSIC",
            AudioManager.STREAM_ALARM to "ALARM",
            AudioManager.STREAM_NOTIFICATION to "NOTIFICATION",
            AudioManager.STREAM_DTMF to "DTMF"
        )
        streams.forEach { (s, name) ->
            runCatching {
                sb.appendLine("  $name: cur=${am?.getStreamVolume(s)} max=${am?.getStreamMaxVolume(s)}")
            }
        }
        runCatching {
            val pkgs = context.packageManager.getInstalledApplications(0).map { it.packageName }
            val vendor = pkgs.filter { p ->
                p.startsWith("com.microntek") || p.startsWith("com.hct") || p.startsWith("com.navimods") ||
                p.startsWith("com.syu") || p.startsWith("com.forfan") || p.startsWith("com.szchoiceway")
            }
            sb.appendLine("  vendor pkgs: ${vendor.joinToString(",")}")
        }
        runCatching {
            val av = android.provider.Settings.Global.getString(context.contentResolver, "av_volume")
            sb.appendLine("  settings.av_volume: $av")
        }
        runCatching {
            sb.appendLine("  getParameters av_volume: ${am?.getParameters("av_volume=")}")
            sb.appendLine("  getParameters cfg_maxvolume: ${am?.getParameters("cfg_maxvolume=")}")
            sb.appendLine("  getParameters mcu_volume: ${am?.getParameters("mcu_volume=")}")
        }
        runCatching {
            val ch = android.provider.Settings.System.getString(context.contentResolver, "av_channel")
            sb.appendLine("  settings.av_channel: $ch")
            val sysAv = android.provider.Settings.System.getString(context.contentResolver, "av_volume")
            sb.appendLine("  settings.system av_volume: $sysAv")
        }
        return sb.toString()
    }
}
