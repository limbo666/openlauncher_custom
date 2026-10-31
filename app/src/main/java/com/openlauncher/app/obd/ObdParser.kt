package com.openlauncher.app.obd

/** Live vehicle values. A null field means the car did not answer for that value. */
data class ObdReading(
    val rpm: Int? = null,
    val speedKmh: Int? = null,
    val coolantC: Int? = null,
    val throttlePct: Int? = null,
    val loadPct: Int? = null,
    val fuelPct: Int? = null,
    val intakeC: Int? = null,
    val batteryVolts: Double? = null
)

/** Standard OBD-II mode 01 parameter IDs read by the widget. */
enum class ObdPid(val command: String, val code: String) {
    RPM("010C", "0C"),
    SPEED("010D", "0D"),
    COOLANT("0105", "05"),
    THROTTLE("0111", "11"),
    LOAD("0104", "04"),
    FUEL("012F", "2F"),
    INTAKE("010F", "0F")
}

/** Pure parsing of ELM327 text replies, kept free of Bluetooth so it can be cleanly tested. */
object ObdParser {
    /**
     * Data bytes that follow the `41 <pid>` header, or null when the reply is not a valid answer.
     * Replies may contain echoes, `SEARCHING...`, `NO DATA`, several ECUs and arbitrary spacing.
     */
    fun dataBytes(response: String, pid: String): List<Int>? {
        val header = "41" + pid.uppercase()
        for (line in response.uppercase().split('\r', '\n')) {
            val hex = line.filter { it in '0'..'9' || it in 'A'..'F' }
            if (!hex.startsWith(header)) continue
            val body = hex.substring(header.length)
            if (body.isEmpty() || body.length % 2 != 0) continue
            return body.chunked(2).map { it.toInt(16) }
        }
        return null
    }

    fun parse(pid: ObdPid, response: String): Int? {
        val bytes = dataBytes(response, pid.code) ?: return null
        return when (pid) {
            ObdPid.RPM -> if (bytes.size >= 2) (bytes[0] * 256 + bytes[1]) / 4 else null
            ObdPid.SPEED -> bytes[0]
            ObdPid.COOLANT, ObdPid.INTAKE -> bytes[0] - 40
            ObdPid.THROTTLE, ObdPid.LOAD, ObdPid.FUEL -> bytes[0] * 100 / 255
        }
    }

    /** Battery voltage from an `ATRV` reply such as `12.6V`; implausible values are rejected. */
    fun parseVoltage(response: String): Double? {
        val match = Regex("""(?<![0-9.])([0-9]{1,2}\.[0-9]{1,2})\s*V""").find(response.uppercase()) ?: return null
        return match.groupValues[1].toDoubleOrNull()?.takeIf { it in 5.0..30.0 }
    }

    fun readingFrom(replies: Map<ObdPid, String>, voltageReply: String?): ObdReading = ObdReading(
        rpm = replies[ObdPid.RPM]?.let { parse(ObdPid.RPM, it) },
        speedKmh = replies[ObdPid.SPEED]?.let { parse(ObdPid.SPEED, it) },
        coolantC = replies[ObdPid.COOLANT]?.let { parse(ObdPid.COOLANT, it) },
        throttlePct = replies[ObdPid.THROTTLE]?.let { parse(ObdPid.THROTTLE, it) },
        loadPct = replies[ObdPid.LOAD]?.let { parse(ObdPid.LOAD, it) },
        fuelPct = replies[ObdPid.FUEL]?.let { parse(ObdPid.FUEL, it) },
        intakeC = replies[ObdPid.INTAKE]?.let { parse(ObdPid.INTAKE, it) },
        batteryVolts = voltageReply?.let { parseVoltage(it) }
    )
}
