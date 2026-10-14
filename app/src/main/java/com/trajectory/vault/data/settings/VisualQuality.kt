package com.trajectory.vault.data.settings

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * How much the app spends on visual effects. Every feature and control stays in all three modes.
 * Performance turns blur and decorative animation off; Low quality keeps blur, transparency and colour
 * overlays but drops the shader effects (lens refraction, light bending); Quality is the full look.
 */
enum class VisualQuality {
    PERFORMANCE,
    LOW,
    QUALITY,
}

/** What the phone's own numbers say about it, used only to pick a starting [VisualQuality]. */
data class DeviceProfile(
    val totalRamGiB: Double,
    val cpuCores: Int,
    val sdk: Int,
    val lowRamFlag: Boolean,
) {
    /**
     * A starting point, not a benchmark: RAM is the main signal because it tracks the phone's class. A
     * phone with 8 GB reports about 7.3 GiB, so only phones from there up start in Quality mode, and only
     * with a recent Android version and more than four CPU cores. Anything else starts in Performance mode.
     */
    val recommended: VisualQuality
        get() = if (!lowRamFlag && totalRamGiB >= 7.0 && cpuCores > 4 && sdk >= Build.VERSION_CODES.Q) {
            VisualQuality.QUALITY
        } else {
            VisualQuality.PERFORMANCE
        }

    companion object {
        private const val BYTES_PER_GIB = 1024.0 * 1024.0 * 1024.0

        fun read(context: Context): DeviceProfile {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val info = ActivityManager.MemoryInfo()
            manager?.getMemoryInfo(info)
            return DeviceProfile(
                totalRamGiB = info.totalMem / BYTES_PER_GIB,
                cpuCores = Runtime.getRuntime().availableProcessors(),
                sdk = Build.VERSION.SDK_INT,
                lowRamFlag = manager?.isLowRamDevice ?: false,
            )
        }
    }
}
