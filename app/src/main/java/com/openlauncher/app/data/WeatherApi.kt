package com.openlauncher.app.data

import com.google.gson.annotations.SerializedName
import com.openlauncher.app.model.DailyForecast
import com.openlauncher.app.model.WeatherState
import com.openlauncher.app.model.dateToDayOfWeek
import com.openlauncher.app.model.mapWeatherToMeteocon
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ── Open-Meteo Models ────────────────────────────────────────────────────────

data class OpenMeteoResponse(
    @SerializedName("current_weather") val currentWeather: CurrentWeather?,
    @SerializedName("daily") val daily: OpenMeteoDaily?,
    @SerializedName("hourly") val hourly: OpenMeteoHourly? = null
)

data class OpenMeteoHourly(
    @SerializedName("time") val time: List<String>?,
    @SerializedName("relative_humidity_2m") val humidity: List<Int>?
)

data class CurrentWeather(
    @SerializedName("temperature")  val temperature: Double,
    @SerializedName("windspeed")    val windspeed: Double,
    @SerializedName("weathercode", alternate = ["weather_code"])  val weathercode: Int,
    @SerializedName("is_day")       val isDay: Int
)

data class OpenMeteoDaily(
    @SerializedName("time") val time: List<String>?,
    @SerializedName("weathercode", alternate = ["weather_code"]) val weatherCode: List<Int>?,
    @SerializedName("temperature_2m_max") val tempMax: List<Double>?,
    @SerializedName("temperature_2m_min") val tempMin: List<Double>?
)

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude")         latitude: Double,
        @Query("longitude")        longitude: Double,
        @Query("current_weather")  currentWeather: Boolean = true,
        @Query("hourly")           hourly: String = "relative_humidity_2m",
        @Query("daily")            daily: String = "weathercode,temperature_2m_max,temperature_2m_min",
        @Query("timezone")         timezone: String = "auto",
        @Query("temperature_unit") temperatureUnit: String = "celsius",
        @Query("windspeed_unit")   windspeedUnit: String = "kmh",
        @Query("forecast_days")    forecastDays: Int = 7
    ): OpenMeteoResponse
}

// ── Geocoding Models ──────────────────────────────────────────────────────────

data class GeocodingResponse(
    @SerializedName("results") val results: List<GeocodingResult>?
)

data class GeocodingResult(
    @SerializedName("name") val name: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("country") val country: String?
)

interface GeocodingApiService {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 1
    ): GeocodingResponse
}

// ── wttr.in Models ───────────────────────────────────────────────────────────

data class WttrInResponse(
    @SerializedName("current_condition") val currentCondition: List<WttrInCurrent>?,
    @SerializedName("nearest_area") val nearestArea: List<WttrInArea>?,
    @SerializedName("weather") val weather: List<WttrInDaily>?
)

data class WttrInCurrent(
    @SerializedName("temp_C") val tempC: String?,
    @SerializedName("temp_F") val tempF: String?,
    @SerializedName("weatherCode") val weatherCode: String?,
    @SerializedName("weatherDesc") val weatherDesc: List<WttrInValue>?,
    @SerializedName("windspeedKmph") val windspeedKmph: String?,
    @SerializedName("humidity") val humidity: String?
)

data class WttrInArea(
    @SerializedName("areaName") val areaName: List<WttrInValue>?,
    @SerializedName("country") val country: List<WttrInValue>?
)

data class WttrInValue(
    @SerializedName("value") val value: String?
)

data class WttrInDaily(
    @SerializedName("date") val date: String?,
    @SerializedName("maxtempC") val maxTempC: String?,
    @SerializedName("mintempC") val minTempC: String?,
    @SerializedName("maxtempF") val maxTempF: String?,
    @SerializedName("mintempF") val minTempF: String?,
    @SerializedName("hourly") val hourly: List<WttrInHourly>?
)

data class WttrInHourly(
    @SerializedName("time") val time: String?,
    @SerializedName("weatherCode") val weatherCode: String?,
    @SerializedName("weatherDesc") val weatherDesc: List<WttrInValue>?,
    @SerializedName("tempC") val tempC: String?
)

interface WttrInService {
    @GET("{location}")
    suspend fun getWeatherData(
        @Path("location", encoded = true) location: String,
        @Query("format") format: String = "j1"
    ): WttrInResponse
}

// ── Unified Weather Repository ───────────────────────────────────────────────

object WeatherApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val openMeteoService: WeatherApiService = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApiService::class.java)

    val wttrInService: WttrInService = Retrofit.Builder()
        .baseUrl("https://wttr.in/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WttrInService::class.java)

    val geocodingService: GeocodingApiService = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeocodingApiService::class.java)

    /**
     * Unified fetch that supports wttr.in and Open-Meteo, with automatic fallback and 7-day forecast.
     */
    suspend fun fetchWeather(
        useGps: Boolean,
        customLocation: String,
        gpsLat: Double?,
        gpsLon: Double?,
        provider: WeatherProvider,
        isDay: Boolean = true,
        advancedIcons: Boolean = true
    ): WeatherState {
        val validGps = useGps && gpsLat != null && gpsLon != null && gpsLat.isFinite() && gpsLon.isFinite()
        var targetLat: Double? = if (validGps) gpsLat!!.coerceIn(-90.0, 90.0) else null
        var targetLon: Double? = if (validGps) com.openlauncher.app.util.normalizeLongitude(gpsLon!!) else null
        var resolvedLocationName: String = if (validGps) "GPS" else customLocation.trim().ifEmpty { "Athens" }

        // If no GPS coordinates, geocode custom location
        if (targetLat == null || targetLon == null) {
            val queryCity = customLocation.trim().ifEmpty { "Athens" }
            try {
                val geo = geocodingService.searchCity(queryCity)
                val first = geo.results?.firstOrNull()
                if (first != null) {
                    targetLat = first.latitude
                    targetLon = first.longitude
                    resolvedLocationName = first.name ?: queryCity
                }
            } catch (_: Exception) {}
        }

        // If GPS coordinates provided, attempt to resolve real city name
        if (targetLat != null && targetLon != null && resolvedLocationName == "GPS") {
            try {
                val wttrGeo = wttrInService.getWeatherData(String.format(java.util.Locale.US, "%.4f,%.4f", targetLat, targetLon))
                val nearestName = wttrGeo.nearestArea?.firstOrNull()?.areaName?.firstOrNull()?.value
                if (!nearestName.isNullOrBlank()) {
                    resolvedLocationName = nearestName
                }
            } catch (_: Exception) {}
        }

        return if (provider == WeatherProvider.OPEN_METEO) {
            if (targetLat != null && targetLon != null) {
                try {
                    fetchFromOpenMeteo(targetLat, targetLon, isDay, resolvedLocationName, advancedIcons)
                } catch (e: Exception) {
                    fetchFromWttrIn(useGps, customLocation, gpsLat, gpsLon, isDay, advancedIcons)
                }
            } else {
                fetchFromWttrIn(useGps, customLocation, gpsLat, gpsLon, isDay, advancedIcons)
            }
        } else {
            // WTTR_IN
            try {
                val wttrState = fetchFromWttrIn(useGps, customLocation, gpsLat, gpsLon, isDay, advancedIcons)
                // If wttr.in only returned 3 days, enrich with Open-Meteo 7-day forecast if coordinates available
                if (wttrState.dailyForecast.size < 7 && targetLat != null && targetLon != null) {
                    try {
                        val openMeteoForecast = fetchDailyForecastFromOpenMeteo(targetLat, targetLon, advancedIcons)
                        if (openMeteoForecast.isNotEmpty()) {
                            wttrState.copy(dailyForecast = openMeteoForecast)
                        } else {
                            wttrState
                        }
                    } catch (_: Exception) {
                        wttrState
                    }
                } else {
                    wttrState
                }
            } catch (e: Exception) {
                if (targetLat != null && targetLon != null) {
                    fetchFromOpenMeteo(targetLat, targetLon, isDay, resolvedLocationName, advancedIcons)
                } else {
                    throw e
                }
            }
        }
    }

    suspend fun fetchDailyForecastFromOpenMeteo(
        lat: Double,
        lon: Double,
        advancedIcons: Boolean = true
    ): List<DailyForecast> {
        val resp = openMeteoService.getForecast(lat, lon, forecastDays = 7)
        val daily = resp.daily ?: return emptyList()
        if (daily.time == null) return emptyList()
        return daily.time.indices.mapNotNull { i ->
            val date = daily.time.getOrNull(i) ?: return@mapNotNull null
            val day = dateToDayOfWeek(date)
            val code = daily.weatherCode?.getOrNull(i) ?: 0
            val maxT = daily.tempMax?.getOrNull(i) ?: 0.0
            val minT = daily.tempMin?.getOrNull(i) ?: 0.0
            val (dSlug, dLabel) = mapWeatherToMeteocon(code, isDay = true, advancedIcons = advancedIcons)
            DailyForecast(
                date = date,
                dayOfWeek = day,
                maxTempC = maxT,
                minTempC = minT,
                weatherCode = code,
                conditionSlug = dSlug,
                conditionLabel = dLabel
            )
        }
    }

    private suspend fun fetchFromWttrIn(
        useGps: Boolean,
        customLocation: String,
        gpsLat: Double?,
        gpsLon: Double?,
        isDay: Boolean,
        advancedIcons: Boolean = true
    ): WeatherState {
        val validGps = useGps && gpsLat != null && gpsLon != null && gpsLat.isFinite() && gpsLon.isFinite()
        val queryLocation = if (validGps) {
            String.format(java.util.Locale.US, "%.4f,%.4f", gpsLat!!.coerceIn(-90.0, 90.0), com.openlauncher.app.util.normalizeLongitude(gpsLon!!))
        } else {
            customLocation.trim().ifEmpty { "Athens" }
        }

        val resp = wttrInService.getWeatherData(queryLocation)
        val cur = resp.currentCondition?.firstOrNull() ?: throw IllegalStateException("No current condition from wttr.in")
        val code = cur.weatherCode?.toIntOrNull() ?: 113
        val curDesc = cur.weatherDesc?.firstOrNull()?.value
        val (slug, label) = mapWeatherToMeteocon(code, isDay, curDesc, advancedIcons)
        val tempC = cur.tempC?.toDoubleOrNull() ?: 0.0
        val windKmh = cur.windspeedKmph?.toDoubleOrNull() ?: 0.0
        val hum = cur.humidity?.toIntOrNull() ?: 0
        val locName = resp.nearestArea?.firstOrNull()?.areaName?.firstOrNull()?.value
            ?: customLocation.ifEmpty { "Local" }

        val forecasts = resp.weather?.mapNotNull { daily ->
            val dDate = daily.date ?: return@mapNotNull null
            val dDay = dateToDayOfWeek(dDate)
            val dMax = daily.maxTempC?.toDoubleOrNull() ?: tempC
            val dMin = daily.minTempC?.toDoubleOrNull() ?: tempC
            // Use middle-of-day hourly code (noon) or first available
            val noonHour = daily.hourly?.getOrNull(4) ?: daily.hourly?.firstOrNull()
            val dCode = noonHour?.weatherCode?.toIntOrNull() ?: code
            val dDesc = noonHour?.weatherDesc?.firstOrNull()?.value
            val (dSlug, dLabel) = mapWeatherToMeteocon(dCode, isDay = true, dDesc, advancedIcons)
            DailyForecast(
                date = dDate,
                dayOfWeek = dDay,
                maxTempC = dMax,
                minTempC = dMin,
                weatherCode = dCode,
                conditionSlug = dSlug,
                conditionLabel = dLabel
            )
        } ?: emptyList()

        return WeatherState(
            temperatureCelsius = tempC,
            weatherCode = code,
            conditionSlug = slug,
            conditionLabel = curDesc ?: label,
            windspeedKmh = windKmh,
            humidity = hum,
            isDay = isDay,
            locationName = locName,
            dailyForecast = forecasts,
            lastUpdatedEpochMs = System.currentTimeMillis(),
            isOffline = false,
            weatherDesc = curDesc
        )
    }

    private suspend fun fetchFromOpenMeteo(
        lat: Double,
        lon: Double,
        isDay: Boolean,
        locationFallback: String,
        advancedIcons: Boolean = true
    ): WeatherState {
        val resp = openMeteoService.getForecast(lat, lon)
        val cur = resp.currentWeather ?: throw IllegalStateException("No current weather from Open-Meteo")
        val (slug, label) = mapWeatherToMeteocon(cur.weathercode, cur.isDay == 1, advancedIcons = advancedIcons)

        val daily = resp.daily
        val forecasts = if (daily?.time != null) {
            daily.time.indices.mapNotNull { i ->
                val date = daily.time.getOrNull(i) ?: return@mapNotNull null
                val day = dateToDayOfWeek(date)
                val code = daily.weatherCode?.getOrNull(i) ?: cur.weathercode
                val maxT = daily.tempMax?.getOrNull(i) ?: cur.temperature
                val minT = daily.tempMin?.getOrNull(i) ?: cur.temperature
                val (dSlug, dLabel) = mapWeatherToMeteocon(code, isDay = true, advancedIcons = advancedIcons)
                DailyForecast(
                    date = date,
                    dayOfWeek = day,
                    maxTempC = maxT,
                    minTempC = minT,
                    weatherCode = code,
                    conditionSlug = dSlug,
                    conditionLabel = dLabel
                )
            }
        } else emptyList()

        val curHum = resp.hourly?.humidity?.firstOrNull() ?: 0

        return WeatherState(
            temperatureCelsius = cur.temperature,
            weatherCode = cur.weathercode,
            conditionSlug = slug,
            conditionLabel = label,
            windspeedKmh = cur.windspeed,
            humidity = curHum,
            isDay = cur.isDay == 1,
            locationName = locationFallback.ifEmpty { "GPS" },
            dailyForecast = forecasts,
            lastUpdatedEpochMs = System.currentTimeMillis(),
            isOffline = false
        )
    }
}
