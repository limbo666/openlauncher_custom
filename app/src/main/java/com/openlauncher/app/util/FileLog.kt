package com.openlauncher.app.util

import android.content.Context
import java.io.File

/**
 * Minimal process-wide diagnostic log that appends to a plain-text file so it can be
 * exported (Settings → Maintenance → Export Diagnostics Log) without needing ADB.
 * Every method is a safe no-op until [init] has run.
 */
object FileLog {
    @Volatile private var dir: File? = null
    private val timeFmt = java.text.SimpleDateFormat("MM-dd HH:mm:ss.SSS", java.util.Locale.US)

    fun init(context: Context) {
        if (dir == null) {
            val base = context.getExternalFilesDir(null) ?: context.filesDir
            dir = File(base, "logs").apply { mkdirs() }
        }
    }

    @Synchronized
    fun log(tag: String, message: String) {
        val d = dir ?: return
        try {
            val f = File(d, "openlauncher.log")
            if (f.length() > 1_000_000L) f.delete() // rotate when it grows too large
            f.appendText("${timeFmt.format(java.util.Date())}  [$tag] $message\n")
        } catch (_: Throwable) {
        }
    }

    fun logFile(): File? = dir?.let { File(it, "openlauncher.log") }
}
