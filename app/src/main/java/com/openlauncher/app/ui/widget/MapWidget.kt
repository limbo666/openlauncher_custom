package com.openlauncher.app.ui.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.openlauncher.app.data.MapProvider
import com.openlauncher.app.data.MapSearchApi
import com.openlauncher.app.data.MapType
import com.openlauncher.app.data.RouteInfo
import com.openlauncher.app.data.SearchResult
import com.openlauncher.app.util.LocationData
import com.openlauncher.app.util.TapSound
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Official Google "night mode" map style (silver → deep blue/charcoal palette). */
private val NIGHT_STYLE_JSON = """
    [{"elementType":"geometry","stylers":[{"color":"#242f3e"}]},
     {"elementType":"labels.text.fill","stylers":[{"color":"#746855"}]},
     {"elementType":"labels.text.stroke","stylers":[{"color":"#242f3e"}]},
     {"featureType":"administrative.locality","elementType":"labels.text.fill","stylers":[{"color":"#d59563"}]},
     {"featureType":"poi","elementType":"labels.text.fill","stylers":[{"color":"#d59563"}]},
     {"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#263c3f"}]},
     {"featureType":"poi.park","elementType":"labels.text.fill","stylers":[{"color":"#6b9a76"}]},
     {"featureType":"road","elementType":"geometry","stylers":[{"color":"#38414e"}]},
     {"featureType":"road","elementType":"geometry.stroke","stylers":[{"color":"#212a37"}]},
     {"featureType":"road","elementType":"labels.text.fill","stylers":[{"color":"#9ca5b3"}]},
     {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#746855"}]},
     {"featureType":"road.highway","elementType":"geometry.stroke","stylers":[{"color":"#1f2835"}]},
     {"featureType":"road.highway","elementType":"labels.text.fill","stylers":[{"color":"#f3d19c"}]},
     {"featureType":"transit","elementType":"geometry","stylers":[{"color":"#2f3948"}]},
     {"featureType":"transit.station","elementType":"labels.text.fill","stylers":[{"color":"#d59563"}]},
     {"featureType":"water","elementType":"geometry","stylers":[{"color":"#17263c"}]},
     {"featureType":"water","elementType":"labels.text.fill","stylers":[{"color":"#515c6d"}]},
     {"featureType":"water","elementType":"labels.text.stroke","stylers":[{"color":"#17263c"}]}]
""".trimIndent()

/** osmdroid night filter — the same Android Auto-ish dark tint the fork used. */
private val OSM_NIGHT_MATRIX = floatArrayOf(
    -0.8f, 0f, 0f, 0f, 220f,
    0f, -0.8f, 0f, 0f, 225f,
    0f, 0f, -0.8f, 0f, 245f,
    0f, 0f, 0f, 1.0f, 0f
)

/**
 * Map widget with two providers:
 *  - GOOGLE: official Maps SDK (online tiles, traffic, satellite/hybrid/terrain)
 *  - OSM: osmdroid + OpenStreetMap tiles — free, no key, cached for offline use
 * A yellow OFFLINE badge appears on both when the device has no internet.
 */
@Composable
fun MapWidget(
    location: LocationData?,
    bearing: Float,
    mapProvider: MapProvider,
    mapType: MapType,
    showTraffic: Boolean,
    isOnline: Boolean,
    accent: Color,
    isDayMode: Boolean = false,
    editMode: Boolean = false,
    controlScale: Float = 1f,
    onSetMapType: (MapType) -> Unit = {},
    onToggleTraffic: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(baseDensity.density * controlScale, baseDensity.fontScale * controlScale)
    var autoFollow by remember { mutableStateOf(true) }

    // Handlers the shared overlay buttons call, set by whichever engine renders
    var engineZoomIn by remember { mutableStateOf<(() -> Unit)?>(null) }
    var engineZoomOut by remember { mutableStateOf<(() -> Unit)?>(null) }
    var engineShowPlace by remember { mutableStateOf<((Double, Double, String) -> Unit)?>(null) }
    var engineClearPlace by remember { mutableStateOf<(() -> Unit)?>(null) }
    var engineShowRoute by remember { mutableStateOf<((List<Pair<Double, Double>>) -> Unit)?>(null) }
    var engineClearRoute by remember { mutableStateOf<(() -> Unit)?>(null) }

    // ── Search / directions state (online only) ────────────────────────────
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var selectedPlace by remember { mutableStateOf<SearchResult?>(null) }
    var route by remember { mutableStateOf<RouteInfo?>(null) }
    var routing by remember { mutableStateOf(false) }
    var routeFailed by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    fun selectPlace(r: SearchResult) {
        // Never hand bogus coordinates to the map engines
        if (!r.latitude.isFinite() || !r.longitude.isFinite()) return
        TapSound.play()
        searchOpen = false
        selectedPlace = r
        autoFollow = false
        engineShowPlace?.invoke(r.latitude, r.longitude, r.name)
    }

    fun silentClear() {
        selectedPlace = null
        route = null
        routing = false
        routeFailed = false
        engineClearPlace?.invoke()
        engineClearRoute?.invoke()
    }

    fun clearAll() {
        TapSound.play()
        silentClear()
    }

    fun startRoute() {
        val loc = location ?: return
        val dest = selectedPlace ?: return
        if (routing) return  // ignore double-taps while a request is in flight
        TapSound.play()
        routing = true
        routeFailed = false
        scope.launch {
            try {
                val r = MapSearchApi.route(
                    fromLat = loc.latitude, fromLon = loc.longitude,
                    toLat = dest.latitude, toLon = dest.longitude
                )
                if (r != null && r.points.isNotEmpty()) {
                    route = r
                    engineShowRoute?.invoke(r.points)
                } else {
                    route = null
                    routeFailed = true
                }
            } catch (t: Throwable) {
                // Belt & braces — the API layer already catches, this guards the UI state
                route = null
                routeFailed = true
            } finally {
                routing = false
            }
        }
    }

    // Debounced search as the user types
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            searching = false
            searchError = null
            return@LaunchedEffect
        }
        searchError = null
        delay(500)
        searching = true
        val q = searchQuery
        val res = runCatching { MapSearchApi.search(q) }.getOrDefault(emptyList())
        if (searchQuery == q) {
            searching = false
            if (res.isEmpty()) searchError = "No results" else searchResults = res
        }
    }

    // Search exists only while online — going offline closes everything quietly
    LaunchedEffect(isOnline) {
        if (!isOnline) {
            searchOpen = false
            searchQuery = ""
            searchResults = emptyList()
            searching = false
            searchError = null
            silentClear()
        }
    }

    // Provider switch drops stale engine overlays + selection UI
    LaunchedEffect(mapProvider) {
        engineShowPlace = null
        engineClearPlace = null
        engineShowRoute = null
        engineClearRoute = null
        silentClear()
    }

    LaunchedEffect(searchOpen) {
        if (searchOpen) focusRequester.requestFocus()
    }

    // Recenter = resume auto-follow; the engines react to the flag
    LaunchedEffect(autoFollow, mapProvider) {
        if (autoFollow) {
            engineZoomIn = null // recenter happens inside the engine
        }
    }

    Box(modifier = modifier.clip(RoundedCornerShape(4.dp))) {
        // Control-size scale: every overlay (chips, buttons, panels) renders
        // through a scaled density, so Normal=1x and Large=2x "just works"
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
        when (mapProvider) {
            MapProvider.GOOGLE -> GoogleMapContent(
                location = location,
                bearing = bearing,
                mapType = mapType,
                showTraffic = showTraffic,
                isDayMode = isDayMode,
                accent = accent,
                autoFollow = autoFollow,
                onGesture = { autoFollow = false },
                onZoomInReady = { engineZoomIn = it },
                onZoomOutReady = { engineZoomOut = it },
                onPlaceReady = { engineShowPlace = it },
                onClearPlaceReady = { engineClearPlace = it },
                onRouteReady = { engineShowRoute = it },
                onClearRouteReady = { engineClearRoute = it }
            )
            MapProvider.OSM -> OsmMapContent(
                location = location,
                bearing = bearing,
                isDayMode = isDayMode,
                isOnline = isOnline,
                accent = accent,
                autoFollow = autoFollow,
                onGesture = { autoFollow = false },
                onZoomInReady = { engineZoomIn = it },
                onZoomOutReady = { engineZoomOut = it },
                onPlaceReady = { engineShowPlace = it },
                onClearPlaceReady = { engineClearPlace = it },
                onRouteReady = { engineShowRoute = it },
                onClearRouteReady = { engineClearRoute = it }
            )
        }

        // Top-left: provider/map-type chip (Google only cycles types)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (mapProvider == MapProvider.GOOGLE) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable {
                            TapSound.play()
                            onSetMapType(
                                when (mapType) {
                                    MapType.ROADMAP -> MapType.SATELLITE
                                    MapType.SATELLITE -> MapType.HYBRID
                                    MapType.HYBRID -> MapType.TERRAIN
                                    MapType.TERRAIN -> MapType.ROADMAP
                                }
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        mapType.name,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (showTraffic) accent.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.65f))
                        .clickable { TapSound.play(); onToggleTraffic() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "TRAFFIC",
                        color = if (showTraffic) Color.Black else Color.White,
                        fontSize = 9.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "OSM",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Search — available only while online, keeps the widget clutter-free offline
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (searchOpen) accent.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.65f))
                        .clickable {
                            TapSound.play()
                            searchOpen = !searchOpen
                            if (searchOpen) {
                                searchQuery = ""
                                searchResults = emptyList()
                                searchError = null
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "SEARCH",
                        color = if (searchOpen) Color.Black else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Offline badge (both providers) — no internet, tiles from cache / stale
            if (!isOnline) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC8B6D00))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "OFFLINE",
                        color = Color.Yellow,
                        fontSize = 9.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Top-right: zoom controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable {
                        TapSound.play()
                        autoFollow = false
                        engineZoomIn?.invoke()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, "Zoom in", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable {
                        TapSound.play()
                        autoFollow = false
                        engineZoomOut?.invoke()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Remove, "Zoom out", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        // Bottom-right: recenter / resume follow
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { TapSound.play(); autoFollow = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.GpsFixed, "Center on location", tint = Color.White, modifier = Modifier.size(15.dp))
        }

        // ── Search panel (online only) ──────────────────────────────────────
        if (searchOpen) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .width(250.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .clickable { } // swallow taps so the map doesn't move underneath
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it.take(80) },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                        cursorBrush = SolidColor(accent),
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .focusRequester(focusRequester),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text("Search place…", color = Color(0xFF888888), fontSize = 10.sp)
                                }
                                inner()
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { TapSound.play(); searchOpen = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, "Close search", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }

                when {
                    searching -> Text(
                        "SEARCHING…",
                        color = Color(0xFFAAAAAA),
                        fontSize = 8.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    searchError != null -> Text(
                        searchError ?: "No results",
                        color = Color(0xFFFF8A80),
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    searchResults.isEmpty() && searchQuery.isNotBlank() -> Text(
                        "NO RESULTS",
                        color = Color(0xFFAAAAAA),
                        fontSize = 8.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    else -> searchResults.take(5).forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                                .clickable { selectPlace(r) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    r.name,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (r.type.isNotEmpty()) {
                                    Text(
                                        r.type.uppercase(),
                                        color = Color(0xFF999999),
                                        fontSize = 7.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Text(
                    "Results © OpenStreetMap contributors",
                    color = Color(0xFF666666),
                    fontSize = 7.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        // ── Bottom-left: selected place + navigate / clear ──────────────────
        val place = selectedPlace
        if (place != null && !searchOpen) {
            val routeLabel = route?.let {
                "%.1f km · %d min".format(it.distanceKm, ((it.durationSec / 60.0).roundToInt()).coerceAtLeast(1))
            } ?: ""
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        place.name,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 150.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (route != null) accent.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.65f))
                        .clickable { if (!routing) startRoute() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        when {
                            routing -> "ROUTING…"
                            routeFailed -> "NO ROUTE"
                            route != null -> routeLabel
                            else -> "NAVIGATE"
                        },
                        color = if (route != null) Color.Black else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { clearAll() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "✕",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // In edit mode block map gestures so the widget can be dragged
        if (editMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { }
            )
        }
        }
    }
}

// ── Google engine ─────────────────────────────────────────────────────────────

@Composable
private fun GoogleMapContent(
    location: LocationData?,
    bearing: Float,
    mapType: MapType,
    showTraffic: Boolean,
    isDayMode: Boolean,
    accent: Color,
    autoFollow: Boolean,
    onGesture: () -> Unit,
    onZoomInReady: (() -> Unit) -> Unit,
    onZoomOutReady: (() -> Unit) -> Unit,
    onPlaceReady: ((Double, Double, String) -> Unit) -> Unit,
    onClearPlaceReady: (() -> Unit) -> Unit,
    onRouteReady: ((List<Pair<Double, Double>>) -> Unit) -> Unit,
    onClearRouteReady: (() -> Unit) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val playServicesOk = remember {
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) ==
            com.google.android.gms.common.ConnectionResult.SUCCESS
    }

    val mapView = remember { MapView(context) }
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var firstFix by remember { mutableStateOf(false) }
    var positionMarker by remember { mutableStateOf<Marker?>(null) }
    var placeMarker by remember { mutableStateOf<Marker?>(null) }
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE  -> mapView.onCreate(null)
                Lifecycle.Event.ON_START   -> mapView.onStart()
                Lifecycle.Event.ON_RESUME  -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE   -> mapView.onPause()
                Lifecycle.Event.ON_STOP    -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (playServicesOk) {
            mapView.getMapAsync { g ->
                googleMap = g
                g.uiSettings.isZoomControlsEnabled = false
                g.uiSettings.isCompassEnabled = false
                g.uiSettings.isMapToolbarEnabled = false
                g.setOnCameraMoveStartedListener { reason ->
                    if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) onGesture()
                }
                onZoomInReady { runCatching { g.animateCamera(CameraUpdateFactory.zoomIn()) } }
                onZoomOutReady { runCatching { g.animateCamera(CameraUpdateFactory.zoomOut()) } }
                onPlaceReady { lat, lon, name ->
                    runCatching {
                        placeMarker?.remove()
                        placeMarker = g.addMarker(
                            MarkerOptions().position(LatLng(lat, lon)).title(name)
                        )
                        g.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 15f))
                    }
                }
                onClearPlaceReady {
                    runCatching {
                        placeMarker?.remove()
                        placeMarker = null
                    }
                }
                onRouteReady { pts ->
                    runCatching {
                        routePolyline?.remove()
                        routePolyline = g.addPolyline(
                            PolylineOptions()
                                .addAll(pts.map { LatLng(it.first, it.second) })
                                .color(accent.toArgb())
                                .width(10f)
                        )
                    }
                }
                onClearRouteReady {
                    runCatching {
                        routePolyline?.remove()
                        routePolyline = null
                    }
                }
            }
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(googleMap, showTraffic) { googleMap?.isTrafficEnabled = showTraffic }

    LaunchedEffect(googleMap, mapType) {
        googleMap?.mapType = when (mapType) {
            MapType.ROADMAP   -> GoogleMap.MAP_TYPE_NORMAL
            MapType.SATELLITE -> GoogleMap.MAP_TYPE_SATELLITE
            MapType.HYBRID    -> GoogleMap.MAP_TYPE_HYBRID
            MapType.TERRAIN   -> GoogleMap.MAP_TYPE_TERRAIN
        }
    }

    LaunchedEffect(googleMap, isDayMode) {
        val g = googleMap ?: return@LaunchedEffect
        runCatching { g.setMapStyle(if (isDayMode) null else MapStyleOptions(NIGHT_STYLE_JSON)) }
    }

    LaunchedEffect(googleMap, location, bearing, autoFollow) {
        val g = googleMap ?: return@LaunchedEffect
        val loc = location ?: return@LaunchedEffect
        runCatching {
            val pos = LatLng(loc.latitude, loc.longitude)

            if (positionMarker == null) {
                positionMarker = g.addMarker(
                    MarkerOptions()
                        .position(pos)
                        .flat(true)
                        .anchor(0.5f, 0.5f)
                        .icon(BitmapDescriptorFactory.fromBitmap(arrowBitmap(context, accent)))
                )
            } else {
                positionMarker?.position = pos
            }
            positionMarker?.rotation = bearing

            if (autoFollow) {
                if (!firstFix) {
                    firstFix = true
                    g.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, zoomByAccuracy(loc.accuracy)))
                } else {
                    g.animateCamera(CameraUpdateFactory.newLatLng(pos))
                }
                g.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.builder()
                            .target(pos)
                            .zoom(g.cameraPosition.zoom)
                            .bearing(bearing)
                            .tilt(0f)
                            .build()
                    )
                )
            }
        }
    }

    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

    if (!playServicesOk) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF111111)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "GOOGLE PLAY SERVICES REQUIRED FOR THE MAP",
                color = Color(0xFF888888),
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

// ── OSM (osmdroid) engine ─────────────────────────────────────────────────────

@Composable
private fun OsmMapContent(
    location: LocationData?,
    bearing: Float,
    isDayMode: Boolean,
    isOnline: Boolean,
    accent: Color,
    autoFollow: Boolean,
    onGesture: () -> Unit,
    onZoomInReady: (() -> Unit) -> Unit,
    onZoomOutReady: (() -> Unit) -> Unit,
    onPlaceReady: ((Double, Double, String) -> Unit) -> Unit,
    onClearPlaceReady: (() -> Unit) -> Unit,
    onRouteReady: ((List<Pair<Double, Double>>) -> Unit) -> Unit,
    onClearRouteReady: (() -> Unit) -> Unit
) {
    val context = LocalContext.current

    // osmdroid global config: offline-capable tile cache (up to 900 MB)
    LaunchedEffect(Unit) {
        val cfg = org.osmdroid.config.Configuration.getInstance()
        cfg.userAgentValue = context.packageName
        val cacheDir = File(context.cacheDir, "osmdroid_tiles")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        cfg.osmdroidTileCache = cacheDir
        cfg.tileFileSystemCacheMaxBytes = 900L * 1024 * 1024
        cfg.tileFileSystemCacheTrimBytes = 700L * 1024 * 1024
    }

    val mapView = remember {
        org.osmdroid.views.MapView(context).apply {
            setMultiTouchControls(true)
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(16.0)
        }
    }

    var firstFix by remember { mutableStateOf(false) }
    val marker = remember(accent) {
        org.osmdroid.views.overlay.Marker(mapView).apply {
            icon = BitmapDrawable(context.resources, arrowBitmap(context, accent))
            setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_CENTER)
        }
    }
    var placeMarker by remember { mutableStateOf<org.osmdroid.views.overlay.Marker?>(null) }
    var routeOverlay by remember { mutableStateOf<org.osmdroid.views.overlay.Polyline?>(null) }

    DisposableEffect(mapView) {
        // Manual scroll disables auto-follow (animated engine moves don't)
        val listener = object : org.osmdroid.events.MapListener {
            override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                if (event?.source?.isAnimating == false) onGesture()
                return true
            }
            override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean = true
        }
        mapView.addMapListener(listener)
        onZoomInReady { runCatching { mapView.controller.zoomIn() } }
        onZoomOutReady { runCatching { mapView.controller.zoomOut() } }
        onPlaceReady { lat, lon, name ->
            runCatching {
                placeMarker?.let { mapView.overlays.remove(it) }
                val m = org.osmdroid.views.overlay.Marker(mapView).apply {
                    position = org.osmdroid.util.GeoPoint(lat, lon)
                    title = name
                }
                mapView.overlays.add(m)
                placeMarker = m
                mapView.controller.setZoom(15.0)
                mapView.controller.animateTo(org.osmdroid.util.GeoPoint(lat, lon))
                mapView.invalidate()
            }
        }
        onClearPlaceReady {
            runCatching {
                placeMarker?.let { mapView.overlays.remove(it) }
                placeMarker = null
                mapView.invalidate()
            }
        }
        onRouteReady { pts ->
            runCatching {
                routeOverlay?.let { mapView.overlays.remove(it) }
                val poly = org.osmdroid.views.overlay.Polyline().apply {
                    setPoints(pts.map { org.osmdroid.util.GeoPoint(it.first, it.second) })
                    outlinePaint.color = accent.toArgb()
                    outlinePaint.strokeWidth = 10f
                }
                mapView.overlays.add(poly)
                routeOverlay = poly
                mapView.invalidate()
            }
        }
        onClearRouteReady {
            runCatching {
                routeOverlay?.let { mapView.overlays.remove(it) }
                routeOverlay = null
                mapView.invalidate()
            }
        }
        onDispose { mapView.removeMapListener(listener) }
    }

    LaunchedEffect(isOnline) {
        // Offline mode: block all tile HTTP requests, serve cache only
        mapView.setUseDataConnection(isOnline)
        mapView.invalidate()
    }

    LaunchedEffect(location, bearing, autoFollow) {
        val loc = location ?: return@LaunchedEffect
        runCatching {
            val geoPoint = org.osmdroid.util.GeoPoint(loc.latitude, loc.longitude)
            marker.position = geoPoint
            if (!mapView.overlays.contains(marker)) mapView.overlays.add(marker)

            if (autoFollow) {
                mapView.mapOrientation = -bearing
                marker.rotation = 0f
                mapView.controller.animateTo(geoPoint)
            } else {
                marker.rotation = bearing
            }

            if (!firstFix) {
                firstFix = true
                mapView.controller.setZoom(zoomByAccuracy(loc.accuracy).toDouble())
                mapView.controller.setCenter(geoPoint)
            }
            mapView.invalidate()
        }
    }

    LaunchedEffect(isDayMode) {
        if (isDayMode) {
            mapView.overlayManager.tilesOverlay.setColorFilter(null)
        } else {
            mapView.overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(OSM_NIGHT_MATRIX))
        }
        mapView.invalidate()
    }

    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
}

private fun zoomByAccuracy(accuracyMeters: Float): Float = when {
    accuracyMeters <= 0f -> 15f
    accuracyMeters < 15f  -> 17f
    accuracyMeters < 50f  -> 16.5f
    accuracyMeters < 150f -> 15.5f
    else                  -> 15f
}

/** Direction arrow marker bitmap — white disc with an accent arrow. */
private fun arrowBitmap(context: android.content.Context, accent: Color): Bitmap {
    val size = (40 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val disc = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        setShadowLayer(6f, 0f, 3f, android.graphics.Color.argb(80, 0, 0, 0))
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, disc)

    val arrow = Paint().apply {
        isAntiAlias = true
        color = accent.toArgb()
    }
    val path = android.graphics.Path().apply {
        moveTo(size / 2f, size / 3.6f)
        lineTo(size / 1.35f, size / 1.5f)
        lineTo(size / 2f, size / 1.7f)
        lineTo(size / 3.7f, size / 1.5f)
        close()
    }
    canvas.drawPath(path, arrow)
    return bitmap
}
