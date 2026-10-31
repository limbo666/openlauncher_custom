package com.openlauncher.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings

/**
 * Unprivileged "knob sniffer".
 *
 * While armed it watches every Settings.System / Settings.Global change and a set of
 * candidate volume broadcasts, then reports the deltas. On these head units the MCU
 * service usually writes the new loudness into some Settings key when the physical knob
 * turns — that delta reveals the real volume channel, even on a locked, ADB-less unit.
 *
 * This is the discovery mechanism: it needs no root and no ADB, only the normal
 * ContentObserver / receiver registration APIs.
 */
object HeadUnitInspector {
    private var observer: ContentObserver? = null
    private var receiver: BroadcastReceiver? = null
    private var appCtx: Context? = null
    private var sysSnapshot: Map<String, String> = emptyMap()
    private var globalSnapshot: Map<String, String> = emptyMap()

    val isArmed: Boolean get() = observer != null

    fun start(context: Context, onEvent: (String) -> Unit) {
        stop()
        val app = context.applicationContext
        appCtx = app
        val emit: (String) -> Unit = { e -> FileLog.log("Sniffer", e); onEvent(e) }
        sysSnapshot = dump(app, Settings.System.CONTENT_URI)
        globalSnapshot = dump(app, Settings.Global.CONTENT_URI)
        emit("ARMED — System=${sysSnapshot.size} keys, Global=${globalSnapshot.size} keys. Turn the knob now.")

        val obs = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                val newSys = dump(app, Settings.System.CONTENT_URI)
                newSys.forEach { (k, v) ->
                    if (sysSnapshot[k] != v) emit("[System] $k = $v  (was ${sysSnapshot[k] ?: "—"})")
                }
                sysSnapshot = newSys
                val newGlb = dump(app, Settings.Global.CONTENT_URI)
                newGlb.forEach { (k, v) ->
                    if (globalSnapshot[k] != v) emit("[Global] $k = $v  (was ${globalSnapshot[k] ?: "—"})")
                }
                globalSnapshot = newGlb
            }
        }
        observer = obs
        runCatching {
            app.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, obs)
            app.contentResolver.registerContentObserver(Settings.Global.CONTENT_URI, true, obs)
        }

        val filter = IntentFilter().apply {
            listOf(
                "com.microntek.VOLUME_SET",
                "com.microntek.VOLUME_CHANGED",
                "com.microntek.volumechange",
                "com.microntek.control.volume",
                "android.media.VOLUME_CHANGED_ACTION",
                "com.syu.ms.volume",
                "com.syu.volumechange",
                "forfan.intent.action.VOLUMEUP",
                "forfan.intent.action.VOLUMEDN",
                "com.hct.volumechange",
                "com.qzd.volumechange",
                "com.qzd.intent.action.VOLUME_SET"
            ).forEach { addAction(it) }
        }
        val rec = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent ?: return
                val extras = intent.extras?.keySet()
                    ?.joinToString { "$it=${intent.extras?.get(it)}" } ?: "none"
                emit("[Broadcast] ${intent.action}  extras: $extras")
            }
        }
        receiver = rec
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                app.registerReceiver(rec, filter, Context.RECEIVER_EXPORTED)
            } else {
                app.registerReceiver(rec, filter)
            }
        }
    }

    fun stop() {
        val app = appCtx
        observer?.let { o -> runCatching { app?.contentResolver?.unregisterContentObserver(o) } }
        observer = null
        receiver?.let { r -> runCatching { app?.unregisterReceiver(r) } }
        receiver = null
    }

    private fun dump(context: Context, uri: Uri): Map<String, String> {
        val m = mutableMapOf<String, String>()
        runCatching {
            context.contentResolver.query(uri, arrayOf("name", "value"), null, null, null)?.use { c ->
                val ni = c.getColumnIndex("name")
                val vi = c.getColumnIndex("value")
                while (c.moveToNext()) {
                    if (ni >= 0 && vi >= 0) {
                        val k = c.getString(ni) ?: continue
                        m[k] = c.getString(vi) ?: ""
                    }
                }
            }
        }
        return m
    }
}
