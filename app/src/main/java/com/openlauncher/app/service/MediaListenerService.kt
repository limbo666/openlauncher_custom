package com.openlauncher.app.service

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.openlauncher.app.model.MediaAction
import com.openlauncher.app.model.NowPlayingState
import com.openlauncher.app.util.FileLog
import com.openlauncher.app.util.SessionSnapshotCache
import com.openlauncher.app.util.mediaActionActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MediaListenerService : NotificationListenerService() {

    private val controllers = linkedMapOf<MediaSession.Token, MediaController>()
    private val callbacks = mutableMapOf<MediaSession.Token, MediaController.Callback>()
    private val snapshots = SessionSnapshotCache<MediaSession.Token, List<Any?>, NowPlayingState>()
    private val handler = Handler(Looper.getMainLooper())
    private val sessionManager by lazy { getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager }
    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { refreshNowPlaying() }

    // Resolving custom-action icon names touches the source app's resources
    // (a binder + resource-table lookup). Cache by package+icon id so the
    // per-refresh resolution is a map hit instead of a binder round-trip.
    private val actionIconNames = object : LinkedHashMap<String, String>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean = size > 128
    }
    private var pendingArtwork: MediaSession.Token? = null
    private val REFRESH_MSG = 1

    /**
     * Coalesces bursts of refresh triggers into one refresh ~350 ms later.
     * Notification posts/removes fire for EVERY system notification and
     * playback position ticks arrive about once per second — without this the
     * full metadata walk would run for each of them.
     */
    private fun scheduleRefresh(artworkToken: MediaSession.Token? = null) {
        if (artworkToken != null) pendingArtwork = artworkToken
        handler.removeCallbacksAndMessages(REFRESH_MSG)
        handler.postDelayed({
            val token = pendingArtwork
            pendingArtwork = null
            refreshNowPlaying(token)
        }, 350L)
    }

    private fun disconnectSessions() {
        runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsChanged) }
        controllers.forEach { (token, controller) -> callbacks[token]?.let { runCatching { controller.unregisterCallback(it) } } }
        callbacks.clear()
        snapshots.clear()
        controllers.clear()
        _sessions.value = emptyList()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        FileLog.init(applicationContext)
        instance = this
        isConnected.value = true
        runCatching {
            sessionManager.addOnActiveSessionsChangedListener(
                sessionsChanged,
                ComponentName(this, MediaListenerService::class.java),
                handler
            )
        }
        refreshNowPlaying()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        isConnected.value = false
        disconnectSessions()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?)  { scheduleRefresh() }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) { scheduleRefresh() }

    override fun onDestroy() {
        instance = null
        disconnectSessions()
        isConnected.value = false
        super.onDestroy()
    }

    private fun refreshNowPlaying(changedArtwork: MediaSession.Token? = null) {
        val active = runCatching {
            sessionManager.getActiveSessions(ComponentName(this, MediaListenerService::class.java))
        }.getOrDefault(emptyList())

        val tokens = active.map { it.sessionToken }.toSet()
        controllers.keys.filter { it !in tokens }.forEach { token ->
            val removed = controllers.remove(token)
            callbacks.remove(token)?.let { runCatching { removed?.unregisterCallback(it) } }
        }

        active.forEach { controller ->
            if (controller.sessionToken !in controllers) {
                controllers[controller.sessionToken] = controller
                val token = controller.sessionToken
                val callback = object : MediaController.Callback() {
                    private var lastStateSig: Pair<Int, Long>? = null
                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        // Position ticks arrive ~1/s; only state/actions affect
                        // what we show (the widget reads position itself).
                        val sig = (state?.state ?: -1) to (state?.actions ?: 0L)
                        if (sig == lastStateSig) return
                        lastStateSig = sig
                        scheduleRefresh()
                    }
                    override fun onMetadataChanged(metadata: MediaMetadata?) = scheduleRefresh(artworkToken = token)
                    override fun onSessionDestroyed() = scheduleRefresh()
                }
                callbacks[token] = callback
                controller.registerCallback(callback, handler)
            }
        }

        snapshots.retain(tokens)
        val states = active.mapNotNull { controller ->
            stateFromController(controller, controller.sessionToken == changedArtwork)
        }
        if (_sessions.value != states) _sessions.value = states
    }

    private fun stateFromController(controller: MediaController, forceArtwork: Boolean): NowPlayingState {
        val meta = controller.metadata
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Unknown"
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        // Prefer the largest bitmap the source provides; many apps put a
        // downscaled image in ALBUM_ART and the full one in ART (or vice versa).
        // DISPLAY_ICON is deliberately excluded — it's the app's own icon, not
        // per-station/track artwork, and was causing a stale logo across presets.
        val art = try {
            listOfNotNull(
                meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART),
                meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ).maxByOrNull { it.width * it.height }
        } catch (_: Exception) { null }

        val artUri = meta?.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
        // Diagnostic: log only on a real metadata change so preset switches can be
        // traced (does the art URI change, or does the source publish no new art?).
        if (forceArtwork) {
            val msg = "pkg=${controller.packageName} title=$title artist=$artist artUri=${artUri ?: "null"} bmp=${art?.let { "${it.width}x${it.height}" } ?: "none"}"
            android.util.Log.d("NowPlayingArt", msg)
            FileLog.log("NowPlayingArt", msg)
        }
        val playback = controller.playbackState
        val isPlaying = playback?.state == PlaybackState.STATE_PLAYING
        val actions = playback?.actions ?: 0L
        // Some players (Spotify, YouTube Music, …) publish their audio session id in
        // the PlaybackState extras so an audio effect can attach to their stream.
        // It is stored as either an Int or a Long depending on the app.
        val audioSessionId = playback?.extras?.let { e ->
            val asInt = e.getInt("android.media.session.extra.AUDIO_SESSION_ID", -1)
            if (asInt > 0) asInt else e.getLong("android.media.session.extra.AUDIO_SESSION_ID", -1L).toInt()
        } ?: -1
        val rating = meta?.getRating(MediaMetadata.METADATA_KEY_USER_RATING)
        val customActions = playback?.customActions.orEmpty().map {
            val iconName = actionIconNames.getOrPut("${controller.packageName}:${it.icon}") {
                runCatching {
                    packageManager.getResourcesForApplication(controller.packageName).getResourceEntryName(it.icon)
                }.getOrDefault("")
            }
            MediaAction(
                id = it.action,
                label = it.name.toString(),
                icon = it.icon,
                active = mediaActionActive(it.name.toString(), iconName, it.extras, rating)
            )
        }
        val ratingStyle = controller.ratingType

        val signature = listOf(
            title, artist, artUri, isPlaying, art != null, actions, customActions, ratingStyle, rating?.toString(),
            meta?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID), meta?.getString(MediaMetadata.METADATA_KEY_ALBUM),
            meta?.getLong(MediaMetadata.METADATA_KEY_DURATION), audioSessionId
        )

        return snapshots.value(controller.sessionToken, signature, forceArtwork) {
            NowPlayingState(
                title         = title,
                artist        = artist,
                album         = album,
                mediaId       = meta?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) ?: "",
                audioSessionId = audioSessionId,
                albumArt      = art,
                artUri        = artUri,
                isPlaying     = isPlaying,
                controller    = controller,
                actions       = actions,
                customActions = customActions,
                ratingStyle   = ratingStyle,
                userRating    = rating
            )
        }
    }

    companion object {
        private val _sessions = MutableStateFlow<List<NowPlayingState>>(emptyList())
        val sessions: StateFlow<List<NowPlayingState>> = _sessions
        val isConnected = MutableStateFlow(false)

        @Volatile private var instance: MediaListenerService? = null
        fun requestRefresh() { instance?.let { service -> service.handler.post { service.refreshNowPlaying() } } }
    }
}
