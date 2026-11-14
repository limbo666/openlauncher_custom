package com.openlauncher.app.data

import com.google.gson.annotations.SerializedName
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Free map search + routing, built entirely on OpenStreetMap infrastructure:
 *  - Search: Nominatim (OSM's geocoder) — free, no key, fair-use 1 req/s
 *  - Routing: Valhalla (open-source router, public FOSSGIS server) — free, no key
 * No Google APIs are called from here, so this never touches the paid Maps
 * web services — even in GOOGLE provider mode the map stays free.
 */

data class SearchResult(
    val name: String,
    val type: String,
    val latitude: Double,
    val longitude: Double
)

data class RouteInfo(
    val points: List<Pair<Double, Double>>,
    val distanceKm: Double,
    val durationSec: Double
)

// ── Nominatim ─────────────────────────────────────────────────────────────────

private interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") q: String,
        @Query("format") format: String = "jsonv2",
        @Query("limit") limit: Int = 5,
        @Query("addressdetails") addressdetails: Int = 0
    ): List<NominatimResult>
}

private data class NominatimResult(
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("lat") val lat: String?,
    @SerializedName("lon") val lon: String?
)

// ── Valhalla ──────────────────────────────────────────────────────────────────

private interface ValhallaApi {
    @POST("route")
    suspend fun route(@Body body: ValhallaRequest): ValhallaResponse
}

private data class ValhallaRequest(
    @SerializedName("locations") val locations: List<ValhallaLocation>,
    @SerializedName("costing") val costing: String = "auto",
    @SerializedName("directions_options") val directionsOptions: Map<String, String> =
        mapOf("units" to "kilometers")
)

private data class ValhallaLocation(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double
)

private data class ValhallaResponse(
    @SerializedName("trip") val trip: ValhallaTrip?
)

private data class ValhallaTrip(
    @SerializedName("summary") val summary: ValhallaSummary?,
    @SerializedName("legs") val legs: List<ValhallaLeg>?
)

private data class ValhallaSummary(
    @SerializedName("length") val length: Double?,
    @SerializedName("time") val time: Double?
)

private data class ValhallaLeg(
    @SerializedName("shape") val shape: String?
)

// ── Service ───────────────────────────────────────────────────────────────────

object MapSearchApi {

    // Nominatim requires a meaningful User-Agent; also keeps us good citizens
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "OpenLauncher-Custom/1.0 (personal car head-unit launcher)")
                    .build()
            )
        }
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val nominatim: NominatimApi = Retrofit.Builder()
        .baseUrl("https://nominatim.openstreetmap.org/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(NominatimApi::class.java)

    private val valhalla: ValhallaApi = Retrofit.Builder()
        .baseUrl("https://valhalla1.openstreetmap.de/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ValhallaApi::class.java)

    /** Search places — returns up to 5 matches or an empty list on any failure. */
    suspend fun search(query: String): List<SearchResult> {
        // Sanitize: trim, drop control characters, cap the length (keeps the
        // request small and well-formed no matter what the text field holds)
        val clean = query
            .filter { it.code >= 32 }
            .trim()
            .take(80)
        if (clean.isBlank()) return emptyList()
        return runCatching {
            nominatim.search(q = clean).mapNotNull { r ->
                val lat = r.lat?.toDoubleOrNull() ?: return@mapNotNull null
                val lon = r.lon?.toDoubleOrNull() ?: return@mapNotNull null
                if (!lat.isFinite() || !lon.isFinite()) return@mapNotNull null
                val title = r.displayName?.substringBefore(",")?.trim()
                    ?: r.name?.trim()
                    ?: clean
                SearchResult(
                    name = title,
                    type = r.type?.replace("_", " ") ?: "",
                    latitude = lat,
                    longitude = lon
                )
            }
        }.getOrDefault(emptyList())
    }

    /** Car route between two points, or null on failure. */
    suspend fun route(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): RouteInfo? {
        // Guard against un-fixed / bogus GPS coordinates before hitting the server
        if (!fromLat.isFinite() || !fromLon.isFinite() || !toLat.isFinite() || !toLon.isFinite()) return null
        if (fromLat == 0.0 && fromLon == 0.0) return null
        return runCatching {
            val resp = valhalla.route(
                ValhallaRequest(
                    listOf(
                        ValhallaLocation(fromLat, fromLon),
                        ValhallaLocation(toLat, toLon)
                    )
                )
            )
            val shape = resp.trip?.legs?.firstOrNull()?.shape ?: return@runCatching null
            val summary = resp.trip?.summary
            RouteInfo(
                points = decodePolyline6(shape),
                distanceKm = (summary?.length ?: 0.0).takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0,
                durationSec = (summary?.time ?: 0.0).takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
            )
        }.getOrNull()
    }

    /** Valhalla's "polyline6" encoding — standard algorithm at 1e-6 precision. */
    private fun decodePolyline6(encoded: String): List<Pair<Double, Double>> {
        val points = ArrayList<Pair<Double, Double>>()
        var index = 0
        var lat = 0L
        var lng = 0L
        // Hard cap — guards against malformed/hostile geometry blowing up memory
        val maxPoints = 4000
        while (index < encoded.length && points.size < maxPoints) {
            var result = 0L
            var shift = 0
            var b: Int
            do {
                if (index >= encoded.length) return points  // truncated stream
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f).toLong() shl shift)
                shift += 5
            } while (b >= 0x20)
            lat += if (result and 1L != 0L) (result shr 1).inv() else (result shr 1)

            result = 0L
            shift = 0
            do {
                if (index >= encoded.length) return points  // truncated stream
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f).toLong() shl shift)
                shift += 5
            } while (b >= 0x20)
            lng += if (result and 1L != 0L) (result shr 1).inv() else (result shr 1)

            points.add(Pair(lat / 1e6, lng / 1e6))
        }
        return points
    }
}
