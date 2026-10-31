package com.openlauncher.app.util

import android.content.Context
import android.net.Uri

/**
 * Bulletproof SAF export.
 *
 * Head-unit DocumentsProviders are often flaky: `openOutputStream` can return null or
 * silently drop bytes, leaving a 0-byte file. This helper:
 *   1. always writes a guaranteed copy to app storage (so nothing is ever lost), then
 *   2. writes + flushes + verifies the SAF uri,
 * and returns a human-readable error instead of swallowing the failure.
 *
 * @return null on success, otherwise an error message.
 */
object SafeExport {
    fun write(context: Context, uri: Uri, text: String, tag: String): String? {
        // Guaranteed on-device fallback, independent of the SAF provider.
        FileLog.writeTextFile("${tag}_fallback.txt", text)
        val bytes = text.toByteArray(Charsets.UTF_8)

        val writeError: String? = try {
            val os = context.contentResolver.openOutputStream(uri, "wt")
            if (os == null) {
                "openOutputStream returned null"
            } else {
                os.use { out ->
                    out.write(bytes)
                    out.flush()
                }
                null
            }
        } catch (t: Throwable) {
            "write failed: ${t.message ?: t.javaClass.simpleName}"
        }
        if (writeError != null) {
            FileLog.log("Export", "$tag FAILED: $writeError")
            return writeError
        }

        // Verify the bytes actually landed in the document.
        val verified = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }?.size ?: -1
        } catch (t: Throwable) {
            -1
        }
        FileLog.log("Export", "$tag wrote=${bytes.size} verified=$verified")
        return if (verified == bytes.size) null else "size mismatch (wrote ${bytes.size}, verified $verified)"
    }
}
