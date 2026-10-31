package com.openlauncher.app.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val speedMps: Float = 0f,
    val elapsedRealtimeMs: Long = 0L,
    val travelBearing: Float? = null
)

fun normalizeLongitude(lon: Double): Double {
    if (!lon.isFinite()) return 0.0
    var n = ((lon + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
    if (n == -180.0 && lon > 0) n = 180.0
    return n
}

class LocationCompassManager(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager   = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val prefs           = context.getSharedPreferences("openlauncher_location_cache", Context.MODE_PRIVATE)

    private val _location  = MutableStateFlow<LocationData?>(null)
    private val _bearing   = MutableStateFlow(0f)
    val location: StateFlow<LocationData?> = _location
    val bearing: StateFlow<Float> = _bearing

    private val gravity      = FloatArray(3)
    private val geomagnetic  = FloatArray(3)
    // Circular low-pass filter for smooth bearing (avoids 0°/360° wrap artifacts)
    private var bearingSin   = 0f
    private var bearingCos   = 1f   // initial: pointing north
    private var lastLocationForBearing: Location? = null
    private var lastEmittedBearing = 0f

    init {
        // Restore cached location immediately on boot so widgets aren't blank
        val lat = prefs.getString("last_lat", null)?.toDoubleOrNull()
        val lon = prefs.getString("last_lon", null)?.toDoubleOrNull()
        val alt = prefs.getString("last_alt", null)?.toDoubleOrNull()
        val cachedBearing = prefs.getFloat("last_bearing", 0f)
        if (cachedBearing != 0f) {
            _bearing.value = cachedBearing
            lastEmittedBearing = cachedBearing
        }
        if (lat != null && lon != null) {
            val normLat = lat.coerceIn(-90.0, 90.0)
            val normLon = normalizeLongitude(lon)
            _location.value = LocationData(
                latitude  = normLat,
                longitude = normLon,
                altitude  = alt ?: 0.0,
                accuracy  = 0f,
                speedMps  = 0f
            )
        }
    }

    private fun remapMatrixForDisplay(inR: FloatArray, outR: FloatArray) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
        val rotation = wm?.defaultDisplay?.rotation ?: android.view.Surface.ROTATION_0
        when (rotation) {
            android.view.Surface.ROTATION_90 -> {
                SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, outR)
            }
            android.view.Surface.ROTATION_180 -> {
                SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, outR)
            }
            android.view.Surface.ROTATION_270 -> {
                SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, outR)
            }
            else -> {
                System.arraycopy(inR, 0, outR, 0, 9)
            }
        }
    }

    private fun emitBearingIfChanged(newBearing: Float) {
        val diff = abs(newBearing - lastEmittedBearing)
        val angleDiff = minOf(diff, 360f - diff)
        if (angleDiff >= 1.0f) {
            lastEmittedBearing = newBearing
            _bearing.value = newBearing
        }
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                val r = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(r, event.values)
                val outR = FloatArray(9)
                remapMatrixForDisplay(r, outR)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(outR, orientation)
                val azimuthRad = orientation[0].toDouble()
                val alpha = 0.12f
                bearingSin = alpha * sin(azimuthRad).toFloat() + (1f - alpha) * bearingSin
                bearingCos = alpha * cos(azimuthRad).toFloat() + (1f - alpha) * bearingCos
                val computed = ((Math.toDegrees(atan2(bearingSin.toDouble(), bearingCos.toDouble())) + 360) % 360).toFloat()
                emitBearingIfChanged(computed)
                return
            }

            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, gravity, 0, 3)
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                }
            }
            val r = FloatArray(9)
            val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                val outR = FloatArray(9)
                remapMatrixForDisplay(r, outR)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(outR, orientation)
                val azimuthRad = orientation[0].toDouble()
                // Circular low-pass filter — correctly handles 0°/360° wrap-around
                val alpha = 0.10f
                bearingSin = alpha * sin(azimuthRad).toFloat() + (1f - alpha) * bearingSin
                bearingCos = alpha * cos(azimuthRad).toFloat() + (1f - alpha) * bearingCos
                val computed = ((Math.toDegrees(atan2(bearingSin.toDouble(), bearingCos.toDouble())) + 360) % 360).toFloat()
                emitBearingIfChanged(computed)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun usableGps(fix: Location): Boolean =
        fix.provider == LocationManager.GPS_PROVIDER &&
        fix.hasAccuracy() && fix.accuracy.isFinite() && fix.accuracy in 0f..50f

    private fun inferredTravelSpeed(previous: Location?, current: Location): Float? {
        if (previous == null || !usableGps(previous) || !usableGps(current)) return null
        val age = (current.elapsedRealtimeNanos - previous.elapsedRealtimeNanos) / 1_000_000
        if (age !in 1..15_000) return null
        val distance = previous.distanceTo(current)
        if (distance < maxOf(8f, previous.accuracy + current.accuracy)) return null
        return (distance / (age / 1000f)).takeIf { it >= 2f }
    }

    private fun travelHeading(previous: Location?, current: Location): Float? {
        if (!usableGps(current)) return null
        if (current.hasBearing() && current.bearing.isFinite()) {
            return (current.bearing % 360f + 360f) % 360f
        }
        if (inferredTravelSpeed(previous, current) == null) return null
        return (previous!!.bearingTo(current) % 360f + 360f) % 360f
    }

    private fun acceptLocationFix(previous: LocationData?, timestamp: Long): Boolean {
        if (timestamp < 0) return false
        if (previous == null || previous.elapsedRealtimeMs == 0L) return true
        if (timestamp < previous.elapsedRealtimeMs) return false
        return true
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            if (!loc.latitude.isFinite() || !loc.longitude.isFinite()) return
            val normLat = loc.latitude.coerceIn(-90.0, 90.0)
            val normLon = normalizeLongitude(loc.longitude)
            val fixTime = if (loc.elapsedRealtimeNanos > 0L) loc.elapsedRealtimeNanos / 1_000_000L else System.currentTimeMillis()
            if (!acceptLocationFix(_location.value, fixTime)) return

            val course = travelHeading(lastLocationForBearing, loc)
            val inferredSpeed = inferredTravelSpeed(lastLocationForBearing, loc)
            val speed = if (loc.hasSpeed()) loc.speed else inferredSpeed ?: 0f
            val altitude = if (loc.hasAltitude()) loc.altitude else (_location.value?.altitude ?: loc.altitude)

            _location.value = LocationData(
                latitude  = normLat,
                longitude = normLon,
                altitude  = altitude,
                accuracy  = loc.accuracy,
                speedMps  = speed,
                elapsedRealtimeMs = fixTime,
                travelBearing = course ?: if (loc.hasBearing()) loc.bearing else null
            )

            // Cache for instant restore on next boot
            prefs.edit()
                .putString("last_lat", normLat.toString())
                .putString("last_lon", normLon.toString())
                .putString("last_alt", altitude.toString())
                .apply()

            if (course != null) {
                emitBearingIfChanged(course)
                prefs.edit().putFloat("last_bearing", course).apply()
            } else if (loc.hasBearing() && loc.bearing.isFinite()) {
                emitBearingIfChanged(loc.bearing)
                prefs.edit().putFloat("last_bearing", loc.bearing).apply()
            }

            val anchor = lastLocationForBearing
            if (loc.provider == LocationManager.GPS_PROVIDER && (anchor == null || !anchor.hasAccuracy() || anchor.accuracy !in 0f..50f || inferredSpeed != null ||
                    fixTime - (anchor.elapsedRealtimeNanos / 1_000_000L) > 15_000L)) {
                lastLocationForBearing = Location(loc)
            }
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun start() {
        stop()

        // Sensors: prefer Rotation Vector; only fallback to Accelerometer + Magnetometer if Rotation Vector is unavailable
        val rotVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotVectorSensor != null) {
            sensorManager.registerListener(sensorListener, rotVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
            }
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
                sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
            }
        }

        // Location — Robust offline-first registration
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        for (provider in providers) {
            try {
                if (locationManager.allProviders.contains(provider)) {
                    locationManager.requestLocationUpdates(
                        provider, 1000L, 0f, locationListener
                    )
                    locationManager.getLastKnownLocation(provider)?.let {
                        locationListener.onLocationChanged(it)
                    }
                }
            } catch (_: SecurityException) {
            } catch (_: Exception) {}
        }
    }

    fun stop() {
        sensorManager.unregisterListener(sensorListener)
        locationManager.removeUpdates(locationListener)
        lastLocationForBearing = null
    }

    fun refreshLocation() {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        for (provider in providers) {
            try {
                if (locationManager.allProviders.contains(provider)) {
                    val last = locationManager.getLastKnownLocation(provider)
                    if (last != null) {
                        locationListener.onLocationChanged(last)
                        break
                    }
                }
            } catch (_: SecurityException) {
            } catch (_: Exception) {}
        }
    }
}
