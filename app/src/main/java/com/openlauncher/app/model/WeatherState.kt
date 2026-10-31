package com.openlauncher.app.model

import java.util.Calendar

data class DailyForecast(
    val date: String,             // e.g. "2026-10-28"
    val dayOfWeek: String,         // e.g. "WED"
    val maxTempC: Double,
    val minTempC: Double,
    val weatherCode: Int,
    val conditionSlug: String,     // Meteocons slug e.g. "partly-cloudy-day"
    val conditionLabel: String     // e.g. "Partly Cloudy"
) {
    fun maxTempDisplay(metric: Boolean): String =
        if (metric) "${Math.round(maxTempC)}°"
        else "${Math.round(celsiusToFahrenheit(maxTempC))}°"

    fun minTempDisplay(metric: Boolean): String =
        if (metric) "${Math.round(minTempC)}°"
        else "${Math.round(celsiusToFahrenheit(minTempC))}°"
}

data class WeatherState(
    val temperatureCelsius: Double,
    val weatherCode: Int,
    val conditionSlug: String = "partly-cloudy-day",
    val conditionLabel: String = "Fair",
    val windspeedKmh: Double = 0.0,
    val humidity: Int = 0,
    val isDay: Boolean = true,
    val locationName: String = "",
    val dailyForecast: List<DailyForecast> = emptyList(),
    val lastUpdatedEpochMs: Long = System.currentTimeMillis(),
    val isOffline: Boolean = false,
    val weatherDesc: String? = null
) {
    fun temperatureDisplay(metric: Boolean): String =
        if (metric) "${Math.round(temperatureCelsius)}°C"
        else "${Math.round(celsiusToFahrenheit(temperatureCelsius))}°F"

    val conditionIcon: String get() = wmoCodeToEmoji(weatherCode, isDay)

    fun remap(advancedIcons: Boolean): WeatherState {
        val (newSlug, newLabel) = mapWeatherToMeteocon(weatherCode, isDay, weatherDesc, advancedIcons)
        val newForecast = dailyForecast.map { d ->
            val (dSlug, dLabel) = mapWeatherToMeteocon(d.weatherCode, isDay = true, advancedIcons = advancedIcons)
            d.copy(conditionSlug = dSlug, conditionLabel = dLabel)
        }
        return copy(conditionSlug = newSlug, conditionLabel = newLabel, dailyForecast = newForecast)
    }
}

fun celsiusToFahrenheit(c: Double) = c * 9.0 / 5.0 + 32.0

fun mapWeatherToMeteocon(
    code: Int,
    isDay: Boolean,
    weatherDesc: String? = null,
    advancedIcons: Boolean = true
): Pair<String, String> {
    if (!advancedIcons) {
        // Standard / Simplified mode: maps to broad general weather categories
        return when (code) {
            0, 113 -> (if (isDay) "clear-day" else "clear-night") to (if (isDay) "Sunny" else "Clear")
            1, 2, 116 -> (if (isDay) "partly-cloudy-day" else "partly-cloudy-night") to "Partly Cloudy"
            119 -> "cloudy" to "Cloudy"
            3, 122 -> "overcast" to "Overcast"
            45, 48, 143, 248, 260 -> "fog" to "Foggy"
            51, 53, 55, 56, 57, 185, 263, 266, 281, 284 -> "drizzle" to "Drizzle"
            61, 63, 65, 80, 81, 82, 176, 293, 296, 299, 302, 305, 308, 353, 356, 359 -> "rain" to "Rain"
            66, 67, 182, 311, 314, 317, 362, 365 -> "sleet" to "Sleet"
            71, 73, 75, 77, 85, 86, 179, 227, 230, 320, 323, 326, 329, 332, 335, 338, 368, 371 -> "snow" to "Snow"
            96, 99, 350, 374, 377 -> "hail" to "Hail"
            95, 200, 386, 389, 392, 395 -> (if (code == 389 || code == 96 || code == 99) "thunderstorms-rain" else "thunderstorms") to "Thunderstorm"
            else -> (if (isDay) "partly-cloudy-day" else "partly-cloudy-night") to "Fair"
        }
    }

    // Advanced / Detailed Dynamic mode: highly differentiated animated icons
    val desc = weatherDesc?.lowercase().orEmpty()

    return when {
        // Severe Thunderstorms & Hail
        code in listOf(99, 389) || (desc.contains("thunder") && desc.contains("heavy")) ->
            "thunderstorms-rain" to "Heavy Thunderstorm"
        code in listOf(96, 377) || (desc.contains("thunder") && desc.contains("hail")) ->
            "thunderstorms-rain" to "Thunderstorm Hail"
        code == 386 || (desc.contains("thunder") && (desc.contains("light") || desc.contains("patchy"))) ->
            (if (isDay) "thunderstorms-day-rain" else "thunderstorms-night-rain") to "Thundery Showers"
        code == 392 || (desc.contains("thunder") && desc.contains("snow")) ->
            (if (isDay) "thunderstorms-day-snow" else "thunderstorms-night-snow") to "Thundery Snow"
        code in listOf(95, 200) || desc.contains("thunder") ->
            (if (isDay) "thunderstorms-day" else "thunderstorms-night") to "Thunderstorm"

        // Heavy / Torrential Rain
        code in listOf(65, 82, 308, 359) || desc.contains("torrential") || desc.contains("heavy rain") ->
            (if (isDay) "extreme-day-rain" else "extreme-night-rain") to "Heavy Rain"
        // Moderate Rain
        code in listOf(63, 81, 302, 356) || desc.contains("moderate rain") ->
            "rain" to "Rain"
        // Light Rain & Showers (distinct sun/moon with animated rain)
        code in listOf(61, 80, 176, 293, 296, 299, 353) || desc.contains("light rain") || desc.contains("patchy rain") ->
            (if (isDay) "partly-cloudy-day-rain" else "partly-cloudy-night-rain") to (if (code in listOf(80, 353)) "Light Showers" else "Light Rain")

        // Freezing Drizzle & Sleet
        code in listOf(57, 67, 284, 314, 320, 365) || desc.contains("heavy freezing") || desc.contains("heavy sleet") ->
            "extreme-sleet" to "Heavy Sleet"
        code in listOf(56, 66, 182, 185, 281, 311, 317, 362) || desc.contains("freezing drizzle") || desc.contains("sleet") ->
            (if (isDay) "partly-cloudy-day-sleet" else "partly-cloudy-night-sleet") to "Sleet"

        // Drizzle (light vs dense)
        code == 55 || desc.contains("dense drizzle") || desc.contains("heavy drizzle") ->
            "overcast-drizzle" to "Dense Drizzle"
        code in listOf(51, 53, 263, 266) || desc.contains("drizzle") ->
            (if (isDay) "partly-cloudy-day-drizzle" else "partly-cloudy-night-drizzle") to "Drizzle"

        // Hail / Ice Pellets
        code in listOf(350, 374) || desc.contains("ice pellets") ->
            (if (isDay) "partly-cloudy-day-hail" else "partly-cloudy-night-hail") to "Ice Pellets"

        // Snow (light, moderate, heavy, blowing)
        code in listOf(75, 86, 230, 335, 338, 371) || desc.contains("blizzard") || desc.contains("heavy snow") ->
            (if (isDay) "extreme-day-snow" else "extreme-night-snow") to "Heavy Snow"
        code in listOf(73, 329, 332) || desc.contains("moderate snow") ->
            "snow" to "Snow"
        code == 227 || desc.contains("blowing snow") ->
            "wind-snow" to "Blowing Snow"
        code in listOf(71, 77, 85, 179, 323, 326, 368) || desc.contains("light snow") || desc.contains("patchy snow") ->
            (if (isDay) "partly-cloudy-day-snow" else "partly-cloudy-night-snow") to "Light Snow"

        // Fog, Mist, Haze
        code in listOf(48, 260) || desc.contains("freezing fog") ->
            "overcast-fog" to "Freezing Fog"
        code in listOf(45, 248) || desc.contains("fog") ->
            (if (isDay) "fog-day" else "fog-night") to "Fog"
        code == 143 || desc.contains("mist") ->
            "mist" to "Mist"
        desc.contains("haze") ->
            (if (isDay) "haze-day" else "haze-night") to "Haze"
        desc.contains("smoke") ->
            "smoke" to "Smoke"
        desc.contains("dust") ->
            (if (isDay) "dust-day" else "dust-night") to "Dust"

        // Overcast (Day / Night distinct)
        code in listOf(3, 122) || desc.contains("overcast") ->
            (if (isDay) "overcast-day" else "overcast-night") to "Overcast"
        // Cloudy
        code == 119 || desc.contains("cloudy") ->
            "cloudy" to "Cloudy"
        // Mostly Clear
        code == 1 || desc.contains("mostly clear") || desc.contains("mainly clear") ->
            (if (isDay) "mostly-clear-day" else "mostly-clear-night") to "Mainly Clear"
        // Partly Cloudy
        code in listOf(2, 116) || desc.contains("partly cloudy") ->
            (if (isDay) "partly-cloudy-day" else "partly-cloudy-night") to "Partly Cloudy"
        // Clear / Sunny
        code in listOf(0, 113) || desc.contains("sunny") || desc.contains("clear") ->
            (if (isDay) "clear-day" else "clear-night") to (if (isDay) "Sunny" else "Clear")

        else -> (if (isDay) "partly-cloudy-day" else "partly-cloudy-night") to "Fair"
    }
}

fun dateToDayOfWeek(dateStr: String): String {
    return runCatching {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val cal = Calendar.getInstance()
            cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            val days = arrayOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
            days[cal.get(Calendar.DAY_OF_WEEK) - 1]
        } else ""
    }.getOrDefault("")
}

private fun wmoCodeToEmoji(code: Int, isDay: Boolean): String = when (code) {
    0, 113 -> if (isDay) "☀️" else "🌙"
    1, 2, 116 -> if (isDay) "⛅" else "🌤"
    3, 119, 122 -> "☁️"
    45, 48, 143, 248, 260 -> "🌫️"
    51, 53, 55, 61, 63, 65, 80, 81, 82, 176, 293, 296, 299, 302, 305, 308, 353, 356, 359 -> "🌧️"
    71, 73, 75, 77, 85, 86, 179, 227, 230, 320, 323, 326, 329, 332, 335, 338, 368, 371 -> "❄️"
    95, 96, 99, 200, 386, 389, 392, 395 -> "⛈️"
    else -> "🌡️"
}
