package com.openlauncher.app.obd

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.util.UUID

/** True when this app may talk to paired Bluetooth devices on the running Android version. */
fun hasBluetoothPermission(context: Context): Boolean {
    val needed = if (Build.VERSION.SDK_INT >= 31) Manifest.permission.BLUETOOTH_CONNECT else Manifest.permission.BLUETOOTH
    return ContextCompat.checkSelfPermission(context, needed) == PackageManager.PERMISSION_GRANTED
}

/** A paired Bluetooth device offered as an OBD adapter. */
data class PairedAdapter(val name: String, val address: String)

@SuppressLint("MissingPermission")
fun pairedAdapters(context: Context): List<PairedAdapter> {
    if (!hasBluetoothPermission(context)) return emptyList()
    val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter ?: return emptyList()
    return runCatching {
        adapter.bondedDevices.orEmpty().map { PairedAdapter(it.name ?: it.address, it.address) }.sortedBy { it.name.lowercase() }
    }.getOrDefault(emptyList())
}

/**
 * One Bluetooth connection to an ELM327-compatible adapter. The widget starts it while visible and stops it when it
 * leaves, so nothing holds the adapter when the widget is removed.
 */
object ObdSession {
    enum class Status { IDLE, CONNECTING, CONNECTED, ERROR }

    data class State(
        val status: Status = Status.IDLE,
        val reading: ObdReading = ObdReading(),
        val message: String = ""
    )

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private var job: Job? = null
    private var address: String = ""
    @Volatile private var socket: BluetoothSocket? = null

    @Synchronized
    fun start(context: Context, deviceAddress: String) {
        if (deviceAddress.isEmpty()) return
        if (job?.isActive == true && address == deviceAddress) return
        stop()
        address = deviceAddress
        val appContext = context.applicationContext
        job = scope.launch { run(appContext, deviceAddress) }
    }

    @Synchronized
    fun stop() {
        job?.cancel()
        job = null
        runCatching { socket?.close() }
        socket = null
        address = ""
        _state.value = State()
    }

    @SuppressLint("MissingPermission")
    private suspend fun run(context: Context, deviceAddress: String) {
        var backoffMs = 3_000L
        while (scope.isActive && job?.isActive == true) {
            try {
                if (!hasBluetoothPermission(context)) throw IOException("Bluetooth permission is needed")
                val adapter: BluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                    ?: throw IOException("This device has no Bluetooth")
                if (!adapter.isEnabled) throw IOException("Bluetooth is off")
                _state.value = State(Status.CONNECTING, message = "Connecting to adapter…")
                val device = adapter.getRemoteDevice(deviceAddress)
                val connection = device.createRfcommSocketToServiceRecord(sppUuid)
                socket = connection
                connection.connect()
                val input = connection.inputStream
                val output = connection.outputStream

                suspend fun send(command: String, timeoutMs: Long = 3_000L): String {
                    output.write((command + "\r").toByteArray(Charsets.US_ASCII))
                    output.flush()
                    return readUntilPrompt(input, timeoutMs)
                }

                // Reset, echo off, linefeeds off, spaces off, headers off, automatic protocol.
                for (setup in listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")) send(setup, 4_000L)
                _state.value = State(Status.CONNECTED, message = "Connected")
                backoffMs = 3_000L
                var silentCycles = 0
                while (job?.isActive == true) {
                    val replies = LinkedHashMap<ObdPid, String>()
                    for (pid in ObdPid.entries) replies[pid] = send(pid.command)
                    val voltage = send("ATRV", 1_500L)
                    val reading = ObdParser.readingFrom(replies, voltage)
                    // An engine that is off still answers ATRV; treat a car that answers nothing else as idle, not an error.
                    val answered = reading.rpm != null || reading.coolantC != null || reading.speedKmh != null
                    silentCycles = if (answered || reading.batteryVolts != null) 0 else silentCycles + 1
                    if (silentCycles >= 5) throw IOException("The adapter stopped answering")
                    _state.value = State(Status.CONNECTED, reading, if (answered) "Connected" else "Connected · ignition or engine off")
                    delay(250)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = State(Status.ERROR, message = e.message ?: "Adapter connection failed")
            } catch (e: SecurityException) {
                _state.value = State(Status.ERROR, message = "Bluetooth permission is needed")
            } catch (e: IllegalArgumentException) {
                _state.value = State(Status.ERROR, message = "The saved adapter address is not valid")
            } finally {
                runCatching { socket?.close() }
                socket = null
            }
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(15_000L)
        }
    }

    /** Collect an ELM327 reply up to its `>` prompt, polling so cancellation and timeouts stay responsive. */
    private suspend fun readUntilPrompt(input: InputStream, timeoutMs: Long): String {
        val reply = StringBuilder()
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            if (input.available() > 0) {
                val next = input.read()
                if (next < 0) throw IOException("The adapter closed the connection")
                if (next == '>'.code) return reply.toString()
                reply.append(next.toChar())
            } else {
                delay(15)
            }
        }
        return reply.toString()
    }
}
