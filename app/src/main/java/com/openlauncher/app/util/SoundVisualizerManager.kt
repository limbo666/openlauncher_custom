package com.openlauncher.app.util

import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Captures a frequency spectrum for the visualizer widget.
 *
 * Tries the safe sources in order:
 *   1. A known audio session id (from media-session extras or
 *      AudioPlaybackConfiguration reflection) → `Visualizer(session)`.
 *   2. The output mix → `Visualizer(0)`, which requires the RECORD_AUDIO
 *      permission on most ROMs (granted at runtime by the app).
 *
 * When neither works, [available] stays false and the widget shows an honest
 * "NO AUDIO CAPTURE" state instead of faking a spectrum.
 */
class SoundVisualizerManager {

    companion object {
        /** Fixed resolution the widget downsamples to its configured bar count. */
        const val BIN_COUNT = 64
        private const val TAG = "SoundVisualizer"
        private const val SIGNAL_THRESHOLD = 0.02f       // bin magnitude that counts as "audio present"
        private const val SIGNAL_HOLD_NANOS = 3_500_000_000L // keep "active" this long after it drops
    }

    private val _spectrum = MutableStateFlow(FloatArray(BIN_COUNT))
    val spectrum: StateFlow<FloatArray> = _spectrum.asStateFlow()

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val _status = MutableStateFlow("Stopped")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _hasSignal = MutableStateFlow(false)
    val hasSignal: StateFlow<Boolean> = _hasSignal.asStateFlow()

    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var visualizer: Visualizer? = null

    private var sessionId = -1
    private var recordAudio = false
    private var lastConfigKey = ""
    private val smoothed = FloatArray(BIN_COUNT)
    private var lastEmitNanos = 0L
    private var lastActiveNanos = 0L
    @Volatile private var lastDataNanos = 0L
    @Volatile private var enabled = false
    @Volatile private var attackMs = 40f
    @Volatile private var decayMs = 350f
    @Volatile private var response = 1f
    private var lastFrameNanos = 0L

    // Self-recovering watchdog: if a probe failed, or the attached Visualizer
    // stopped delivering data (e.g. after switching media apps), retry the probe.
    private val watchdog = object : Runnable {
        override fun run() {
            try {
                if (enabled) {
                    val now = System.nanoTime()
                    val stale = _available.value && (now - lastDataNanos > 5_000_000_000L)
                    if (!_available.value || stale) {
                        Log.d(TAG, "watchdog re-probe (available=${_available.value}, stale=$stale)")
                        probeAndStart()
                    }
                }
            } catch (_: Throwable) {
            } finally {
                handler?.postDelayed(this, 4_000)
            }
        }
    }

    /**
     * Re-evaluates the source whenever the widget state, the discovered audio
     * session id, or the RECORD_AUDIO permission changes. [recordAudio] is kept in
     * the key so granting the permission triggers a re-probe of the output mix.
     */
    @Synchronized
    fun configure(enabled: Boolean, sessionId: Int, recordAudio: Boolean, attackMs: Float, decayMs: Float, response: Float) {
        val sourceKey = "$enabled|$sessionId|$recordAudio"
        val sourceChanged = sourceKey != lastConfigKey
        lastConfigKey = sourceKey
        this.enabled = enabled
        this.sessionId = sessionId
        this.recordAudio = recordAudio
        this.attackMs = attackMs
        this.decayMs = decayMs
        this.response = response
        if (!sourceChanged) return // tuning-only change — no re-probe needed
        ensureThread()
        handler?.post { doConfigure() }
    }

    @Synchronized
    fun stop() {
        lastConfigKey = ""
        enabled = false
        stopActive()
        thread?.quitSafely()
        thread = null
        handler = null
        _available.value = false
        _hasSignal.value = false
        lastActiveNanos = 0L
        lastFrameNanos = 0L
        smoothed.fill(0f)
        _status.value = "Stopped"
        _spectrum.value = FloatArray(BIN_COUNT)
    }

    private fun ensureThread() {
        if (thread == null) {
            val t = HandlerThread("SoundVisualizer").apply { start() }
            thread = t
            handler = Handler(t.looper)
            handler?.postDelayed(watchdog, 4_000)
        }
    }

    private fun stopActive() {
        visualizer?.let { v -> runCatching { v.setEnabled(false) }; runCatching { v.release() } }
        visualizer = null
    }

    private fun doConfigure() { // runs on the handler thread
        stopActive()
        if (!enabled) { _available.value = false; _status.value = "Stopped"; return }
        _status.value = "Probing audio sources…"
        probeAndStart()
    }

    private fun probeAndStart() {
        if (sessionId > 0 && startVisualizer(sessionId, "session($sessionId)")) {
            _available.value = true
            _status.value = "Capturing: Visualizer session $sessionId"
            return
        }
        if (startVisualizer(0, "output-mix(0)")) {
            _available.value = true
            _status.value = "Capturing: output mix"
            return
        }
        _available.value = false
        _status.value = if (recordAudio) "No audio source available" else "No capture — grant the microphone/record permission"
    }

    private fun startVisualizer(sessionId: Int, label: String): Boolean {
        Log.d(TAG, "attach target=$label")
        return runCatching {
            val v = Visualizer(sessionId)
            val range = Visualizer.getCaptureSizeRange()
            val captureSize = if (range != null && range.size == 2) range[1] else 1024
            v.captureSize = captureSize
            val listenerOk = v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}
                    override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft != null) onFft(fft)
                    }
                },
                Visualizer.getMaxCaptureRate(),
                false, true
            )
            val enabledOk = v.setEnabled(true)
            Log.d(TAG, "target=$label listener=${statusName(listenerOk)} enabled=${statusName(enabledOk)}")
            if (listenerOk == Visualizer.SUCCESS && enabledOk == Visualizer.SUCCESS) {
                visualizer = v
                true
            } else {
                runCatching { v.release() }
                _status.value = "Failed $label (listener=${statusName(listenerOk)}, enable=${statusName(enabledOk)})"
                false
            }
        }.getOrElse {
            Log.d(TAG, "attach target=$label failed: ${it.javaClass.simpleName}: ${it.message}")
            _status.value = "Failed $label (${it.javaClass.simpleName}: ${it.message})"
            false
        }
    }

    private fun statusName(status: Int): String = when (status) {
        Visualizer.SUCCESS -> "SUCCESS"
        Visualizer.ERROR -> "ERROR"
        Visualizer.ALREADY_EXISTS -> "ALREADY_EXISTS"
        Visualizer.ERROR_NO_INIT -> "ERROR_NO_INIT"
        Visualizer.ERROR_BAD_VALUE -> "ERROR_BAD_VALUE"
        Visualizer.ERROR_INVALID_OPERATION -> "ERROR_INVALID_OPERATION"
        Visualizer.ERROR_DEAD_OBJECT -> "ERROR_DEAD_OBJECT"
        else -> "code=$status"
    }

    private fun onFft(fft: ByteArray) {
        try {
            lastDataNanos = System.nanoTime()
            if (!_available.value) return
            val bins = fft.size
            if (bins < 2) return
            val target = FloatArray(BIN_COUNT)
            for (i in 0 until BIN_COUNT) {
                val t = i.toFloat() / (BIN_COUNT - 1)
                val idx = (t * t * (bins - 1)).toInt().coerceIn(1, bins - 1)
                val mag = (fft[idx].toInt() and 0xFF) / 255f
                target[i] = (mag * 3.5f).coerceIn(0f, 1f)
            }
            publish(target)
        } catch (_: Throwable) {
        }
    }

    private fun publish(target: FloatArray) {
        var max = 0f
        val now = System.nanoTime()
        val dtMs = if (lastFrameNanos == 0L) 0f
                   else ((now - lastFrameNanos) / 1_000_000f).coerceIn(0f, 250f)
        lastFrameNanos = now

        for (i in 0 until BIN_COUNT) {
            val raw = target[i]
            if (raw > max) max = raw
            // Response curve (gamma): >1 spreads the bars apart (less all-peaking),
            // <1 compresses them. Then chase it with a time-constant envelope.
            val shaped = Math.pow(raw.toDouble().coerceIn(0.0, 1.0), response.toDouble())
                .toFloat().coerceIn(0f, 1f)
            val rising = shaped > smoothed[i]
            val tau = if (rising) attackMs else decayMs
            val k = if (tau <= 1f) 1f else (1f - Math.exp((-dtMs / tau).toDouble())).toFloat()
            smoothed[i] += (shaped - smoothed[i]) * k
        }

        // Signal-present detection uses the raw (un-shaped) peak so volume changes
        // still register even when the response curve is steep.
        if (max > SIGNAL_THRESHOLD) lastActiveNanos = now
        _hasSignal.value = (now - lastActiveNanos) < SIGNAL_HOLD_NANOS

        if (now - lastEmitNanos < 33_000_000L) return
        lastEmitNanos = now
        _spectrum.value = smoothed.copyOf()
    }
}
