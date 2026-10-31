package com.openlauncher.app.util

/**
 * Process-wide "volume before the last mute", shared so the Volume and Quick-Actions
 * widgets unmute back to the same value regardless of which one performed the mute.
 * -1 means "no previous value recorded".
 */
object MuteState {
    @Volatile var preMuteVolume: Int = -1
}
