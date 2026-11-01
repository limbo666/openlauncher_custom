package com.openlauncher.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.view.accessibility.AccessibilityEvent

/**
 * Minimal accessibility service whose only job is to perform global actions (the
 * Back key) on behalf of the floating quick-controls panel. Android has no public
 * way for a normal app to inject the Back key into another app, so an accessibility
 * service is the supported route.
 *
 * The user must enable "Open Launcher Controls" in Settings → Accessibility for
 * the Back action to work; otherwise [performBack] is a safe no-op.
 */
class GlobalActionService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile private var instance: GlobalActionService? = null

        /** Returns true if the Back action was performed (service enabled & connected). */
        fun performBack(): Boolean {
            val s = instance ?: return false
            return s.performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }
}
