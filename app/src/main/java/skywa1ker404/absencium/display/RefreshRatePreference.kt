package skywa1ker404.absencium.display

import android.app.Activity
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import skywa1ker404.absencium.logging.DebugLogger

/**
 * Tells Android which display mode the app would like: the highest refresh rate that is at most 90 Hz, at the screen's
 * current resolution. A 60 Hz screen gets 60, a 90 Hz screen 90, and 120 Hz or 144 Hz screens are asked for 90 at most.
 *
 * This is a preference, never an override. It only uses the public `WindowManager.LayoutParams.preferredDisplayModeId`
 * of this app's own window (API 23+). Android stays free to choose another rate: adaptive refresh, battery saver, thermal
 * limits, the user's per-app display settings and manufacturer policy all win, and if the mode is not supported or is
 * ignored nothing else happens. No global setting, system property or hidden API is touched, and nothing runs in the
 * background: the listener exists only while the activity is started.
 */
class RefreshRatePreference(private val activity: Activity) {

    private val displayManager: DisplayManager? by lazy { activity.getSystemService(DisplayManager::class.java) }

    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == currentDisplay()?.displayId) apply()
        }
    }

    /** Call from onStart. Asks for the preferred mode and watches for the display changing (a fold, an external screen). */
    fun start() {
        apply()
        displayManager?.registerDisplayListener(listener, null)
    }

    /** Call from onStop. */
    fun stop() {
        displayManager?.unregisterDisplayListener(listener)
    }

    @Suppress("DEPRECATION")
    private fun currentDisplay(): Display? =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) activity.display else activity.windowManager.defaultDisplay
        } catch (e: Exception) {
            null
        }

    private fun apply() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            val display = currentDisplay() ?: return
            val current = display.mode
            val best = display.supportedModes
                .filter {
                    it.physicalWidth == current.physicalWidth &&
                        it.physicalHeight == current.physicalHeight &&
                        it.refreshRate <= MAX_REFRESH_HZ
                }
                .maxByOrNull { it.refreshRate }
                ?: return
            val window = activity.window
            val attributes = window.attributes
            if (attributes.preferredDisplayModeId == best.modeId) return
            attributes.preferredDisplayModeId = best.modeId
            window.attributes = attributes
            DebugLogger.i(TAG, "Preferred display mode ${best.modeId}: ${best.refreshRate} Hz (screen is at ${current.refreshRate} Hz)")
        } catch (e: Exception) {
            // A preference only: if anything about it fails, Android keeps managing the refresh rate on its own.
            DebugLogger.w(TAG, "Could not set a preferred refresh rate: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "Display"

        /** Never ask for more than 90 Hz. The small margin covers 90 Hz modes reported as 90.0001. */
        const val MAX_REFRESH_HZ = 90.5f
    }
}
