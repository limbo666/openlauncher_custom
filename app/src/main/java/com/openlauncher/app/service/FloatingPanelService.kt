package com.openlauncher.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.openlauncher.app.R
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.DayNightMode
import com.openlauncher.app.data.FloatingActionType
import com.openlauncher.app.data.FloatingPanelAction
import com.openlauncher.app.data.SettingsRepository
import com.openlauncher.app.util.FileLog
import com.openlauncher.app.util.VolumeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Draws the configurable quick-controls panel on top of other apps using
 * SYSTEM_ALERT_WINDOW, rendered with a plain View hierarchy (no Compose) so it
 * works in a WindowManager context without an Activity lifecycle.
 */
class FloatingPanelService : Service() {

    companion object {
        private const val CHANNEL_ID = "floating_panel"
        private const val NOTIF_ID = 2001

        private val launcherVisible = MutableStateFlow(true)

        fun setLauncherVisible(visible: Boolean) {
            launcherVisible.value = visible
        }

        fun start(context: Context) {
            val i = Intent(context, FloatingPanelService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingPanelService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private var panelView: LinearLayout? = null
    private var panelLp: WindowManager.LayoutParams? = null
    private var blackoutView: View? = null
    private var blackedOut = false

    private val repo by lazy { SettingsRepository(this) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()

        if (!Settings.canDrawOverlays(this)) {
            FileLog.log("FloatingPanel", "no overlay permission — stopping")
            stopSelf()
            return
        }

        scope.launch {
            combine(repo.settingsFlow, launcherVisible) { s, vis -> s to vis }
                .collect { (s, visible) ->
                    val shouldShow = s.floatingPanelEnabled && !visible
                    FileLog.log("FloatingPanel", "sync enabled=${s.floatingPanelEnabled} launcherVisible=$visible show=$shouldShow")
                    if (shouldShow) ensurePanel(s) else removePanel()
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onDestroy() {
        removePanel()
        removeBlackout()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Quick Controls", NotificationManager.IMPORTANCE_MIN).apply {
                    setShowBadge(false)
                }
            )
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Quick Controls")
                .setContentText("Floating controls panel")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Quick Controls")
                .setContentText("Floating controls panel")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    // ── Panel ──────────────────────────────────────────────────────────────────
    private fun ensurePanel(s: AppSettings) {
        val existing = panelView
        if (existing == null) {
            addBlackout()
            val panel = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            rebuildContent(panel, s)
            val lp = buildLayoutParams(s)
            runCatching { windowManager.addView(panel, lp) }
                .onSuccess {
                    panelView = panel
                    panelLp = lp
                    FileLog.log("FloatingPanel", "panel added")
                }
                .onFailure {
                    FileLog.log("FloatingPanel", "addView failed: ${it.message}")
                }
        } else {
            rebuildContent(existing, s)
        }
    }

    private fun rebuildContent(panel: LinearLayout, s: AppSettings) {
        val density = resources.displayMetrics.density
        val scale = s.floatingPanelScale
        val padding = (6 * scale * density).toInt()
        val spacing = (4 * scale * density).toInt()
        val buttonSize = (46 * scale * density).toInt()
        val iconSize = (26 * scale * density).toInt()
        val handleWidth = (34 * scale * density).toInt()
        val gripSize = (22 * scale * density).toInt()
        val corner = (26 * scale * density).toInt()
        val accent = s.accentColor
        val vertical = s.floatingPanelVertical

        panel.setPadding(padding, padding, padding, padding)
        panel.background = GradientDrawable().apply {
            cornerRadius = corner.toFloat()
            setColor(Color.argb((s.floatingPanelOpacity * 255).toInt().coerceIn(0, 255), 0, 0, 0))
            setStroke((1 * density).toInt(), accent)
        }
        panel.removeAllViews()
        panel.orientation = if (vertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        panel.gravity = if (vertical) android.view.Gravity.CENTER_HORIZONTAL else android.view.Gravity.CENTER_VERTICAL

        // Drag handle — the only area that registers dragging the panel.
        // Horizontal panel: vertical grip strip on the left. Vertical panel:
        // horizontal grip strip on top.
        val dimAccent = Color.argb(150, Color.red(accent), Color.green(accent), Color.blue(accent))
        val handle = ImageView(this).apply {
            setImageDrawable(drawable(R.drawable.ic_floating_drag_handle))
            imageTintList = ColorStateList.valueOf(dimAccent)
            contentDescription = "Drag to move"
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(
                if (vertical) (buttonSize - gripSize) / 2 else 0,
                if (vertical) 0 else (buttonSize - gripSize) / 2,
                if (vertical) (buttonSize - gripSize) / 2 else 0,
                if (vertical) 0 else (buttonSize - gripSize) / 2
            )
            setBackgroundColor(Color.TRANSPARENT)
        }
        attachDragGesture(handle)
        val handleLp = if (vertical)
            LinearLayout.LayoutParams(buttonSize, handleWidth)
        else
            LinearLayout.LayoutParams(handleWidth, buttonSize)
        panel.addView(handle, handleLp)

        // Action buttons — click only, with press feedback (brighter tint + scale bounce)
        s.floatingPanelActions.forEach { action ->
            val btn = ImageButton(this).apply {
                setImageDrawable(drawable(actionIconRes(action.type)))
                imageTintList = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_pressed),
                        intArrayOf()
                    ),
                    intArrayOf(brighten(accent, 60), accent)
                )
                contentDescription = labelFor(action.type)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(
                    (buttonSize - iconSize) / 2,
                    (buttonSize - iconSize) / 2,
                    (buttonSize - iconSize) / 2,
                    (buttonSize - iconSize) / 2
                )
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { executeAction(action) }
            }
            attachPressBounce(btn)
            val lp = LinearLayout.LayoutParams(buttonSize, buttonSize).apply {
                if (vertical) topMargin = spacing else leftMargin = spacing
            }
            panel.addView(btn, lp)
        }
    }

    private fun actionIconRes(type: FloatingActionType): Int = when (type) {
        FloatingActionType.HOME -> R.drawable.ic_floating_home
        FloatingActionType.VOL_UP -> R.drawable.ic_floating_vol_up
        FloatingActionType.VOL_DOWN -> R.drawable.ic_floating_vol_down
        FloatingActionType.MUTE -> R.drawable.ic_floating_mute
        FloatingActionType.BLACKOUT -> R.drawable.ic_floating_blackout
        FloatingActionType.DAY_NIGHT -> R.drawable.ic_floating_daynight
        FloatingActionType.APP -> R.drawable.ic_floating_apps
    }

    private fun drawable(resId: Int): Drawable = ContextCompat.getDrawable(this, resId)!!.mutate()

    private fun brighten(color: Int, amount: Int): Int =
        Color.argb(
            255,
            (Color.red(color) + amount).coerceAtMost(255),
            (Color.green(color) + amount).coerceAtMost(255),
            (Color.blue(color) + amount).coerceAtMost(255)
        )

    private fun attachPressBounce(btn: ImageButton) {
        btn.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(0.82f).scaleY(0.82f).setDuration(70).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(130).start()
            }
            false // let the button's click listener handle the tap
        }
    }

    private fun attachDragGesture(handle: View) {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        handle.setOnTouchListener { _, ev ->
            val panel = panelView ?: return@setOnTouchListener true
            val lp = panelLp ?: return@setOnTouchListener true
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = ev.rawX
                    downRawY = ev.rawY
                    val loc = IntArray(2)
                    panel.getLocationOnScreen(loc)
                    startX = loc[0]
                    startY = loc[1]
                    lp.gravity = Gravity.TOP or Gravity.START
                    lp.x = startX
                    lp.y = startY
                    runCatching { windowManager.updateViewLayout(panel, lp) }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.gravity = Gravity.TOP or Gravity.START
                    lp.x = startX + (ev.rawX - downRawX).toInt()
                    lp.y = startY + (ev.rawY - downRawY).toInt()
                    runCatching { windowManager.updateViewLayout(panel, lp) }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    persistPosition()
                    true
                }
                else -> true
            }
        }
    }

    private fun labelFor(type: FloatingActionType): String = when (type) {
        FloatingActionType.HOME -> "Home"
        FloatingActionType.VOL_UP -> "Volume up"
        FloatingActionType.VOL_DOWN -> "Volume down"
        FloatingActionType.MUTE -> "Mute"
        FloatingActionType.BLACKOUT -> "Screen blackout"
        FloatingActionType.DAY_NIGHT -> "Day/Night"
        FloatingActionType.APP -> "App"
    }

    private fun buildLayoutParams(s: AppSettings): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (s.floatingPanelPositionSet) {
                Gravity.TOP or Gravity.START
            } else {
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }
            x = if (s.floatingPanelPositionSet) s.floatingPanelX else 0
            y = if (s.floatingPanelPositionSet) s.floatingPanelY else dpToPx(72)
        }
    }

    private fun persistPosition() {
        val lp = panelLp ?: return
        val x = lp.x
        val y = lp.y
        scope.launch {
            runCatching { repo.updateSettings { it.copy(floatingPanelX = x, floatingPanelY = y, floatingPanelPositionSet = true) } }
        }
    }

    private fun removePanel() {
        panelView?.let { v -> runCatching { windowManager.removeView(v) } }
        panelView = null
        panelLp = null
        blackedOut = false
        blackoutView?.alpha = 0f
    }

    private fun addBlackout() {
        if (blackoutView != null) return
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val v = View(this).apply { setBackgroundColor(Color.BLACK); alpha = 0f }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        runCatching { windowManager.addView(v, lp) }
            .onSuccess { blackoutView = v }
            .onFailure { blackoutView = null }
    }

    private fun removeBlackout() {
        blackoutView?.let { v -> runCatching { windowManager.removeView(v) } }
        blackoutView = null
        blackedOut = false
    }

    private fun toggleBlackout() {
        blackedOut = !blackedOut
        blackoutView?.alpha = if (blackedOut) 1f else 0f
    }

    // ── Actions ────────────────────────────────────────────────────────────────
    private fun executeAction(action: FloatingPanelAction) {
        when (action.type) {
            FloatingActionType.HOME -> launchHome()
            FloatingActionType.VOL_UP -> VolumeController.adjust(this, +1)
            FloatingActionType.VOL_DOWN -> VolumeController.adjust(this, -1)
            FloatingActionType.MUTE -> VolumeController.toggleMute(this)
            FloatingActionType.BLACKOUT -> toggleBlackout()
            FloatingActionType.DAY_NIGHT -> toggleDayNight()
            FloatingActionType.APP -> if (action.packageName.isNotBlank()) launchApp(action.packageName)
        }
    }

    private fun launchHome() {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun launchApp(pkg: String) {
        runCatching {
            val i = packageManager.getLaunchIntentForPackage(pkg)
                ?: Intent(Intent.ACTION_MAIN).apply { setPackage(pkg) }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }
    }

    private fun toggleDayNight() {
        scope.launch {
            runCatching {
                val current = repo.settingsFlow.first().dayNightMode
                val next = if (current == DayNightMode.LIGHT) DayNightMode.DARK else DayNightMode.LIGHT
                repo.updateSettings { it.copy(dayNightMode = next) }
            }
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
