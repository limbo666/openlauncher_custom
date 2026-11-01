package com.openlauncher.app.util

import android.net.TrafficStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.roundToInt

data class NetworkSpeedState(
    val rxBytesPerSec: Long = 0L,
    val txBytesPerSec: Long = 0L
) {
    val isIdle: Boolean get() = rxBytesPerSec <= 0L && txBytesPerSec <= 0L
}

/**
 * Samples [TrafficStats] total bytes once per second and emits the instantaneous
 * upload/download rate (bytes/sec). Total covers every active interface (Wi-Fi,
 * cellular, ethernet), which is what we want on head units where a 4G dongle may
 * not be reported through the telephony stack.
 */
class NetworkSpeedManager {

    val speed: Flow<NetworkSpeedState> = flow {
        var prevRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
        var prevTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
        var prevNanos = System.nanoTime()

        while (true) {
            delay(1_000)
            val rx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
            val tx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
            val now = System.nanoTime()
            val dtSec = (now - prevNanos) / 1_000_000_000.0

            val rxRate = if (dtSec > 0.0 && rx >= prevRx) ((rx - prevRx) / dtSec).toLong() else 0L
            val txRate = if (dtSec > 0.0 && tx >= prevTx) ((tx - prevTx) / dtSec).toLong() else 0L

            prevRx = rx
            prevTx = tx
            prevNanos = now
            emit(NetworkSpeedState(rxRate, txRate))
        }
    }
}

fun formatBitRate(bytesPerSec: Long): String {
    val bits = bytesPerSec.coerceAtLeast(0L) * 8L
    return when {
        bits < 1_000L           -> "$bits b/s"
        bits < 1_000_000L       -> "${"%.0f".format(bits / 1_000.0)} Kb/s"
        bits < 1_000_000_000L   -> "${"%.1f".format(bits / 1_000_000.0)} Mb/s"
        else                    -> "${"%.2f".format(bits / 1_000_000_000.0)} Gb/s"
    }
}

/** Compact form for the header indicator, e.g. "2.4M" / "128K" / "0". */
fun formatBitRateShort(bytesPerSec: Long): String {
    val bits = bytesPerSec.coerceAtLeast(0L) * 8L
    return when {
        bits < 1_000L           -> "$bits"
        bits < 1_000_000L       -> "${(bits / 1_000.0).roundToInt()}K"
        bits < 1_000_000_000L   -> "${"%.1f".format(bits / 1_000_000.0)}M"
        else                    -> "${"%.2f".format(bits / 1_000_000_000.0)}G"
    }
}
