package com.trajectory.vault.data.settings

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * How much the app spends on visual effects. Lower levels keep every feature and control and only
 * reduce what is drawn and animated.
 */
enum class VisualQuality {
    LOW,
    MID,
    HIGH,
    ULTRA,
}

/** What the phone's own numbers say about it, used only to pick a starting [VisualQuality]. */
data class DeviceProfile(
    val totalRamGiB: Double,
    val cpuCores: Int,
    val sdk: Int,
    val lowRamFlag: Boolean,
) {
    /**
     * A starting point, not a benchmark: RAM is the main signal because it tracks the phone's class.
     * Ultra is never chosen automatically. A phone with 4 GB reports a little under 4 GiB, a 6 GB phone
     * about 5.5 GiB and an 8 GB phone about 7.3 GiB, so the cut-offs sit between those figures.
     */
    val recommended: VisualQuality
        get() {
            val byRam = when {
                lowRamFlag || totalRamGiB < 4.6 -> VisualQuality.LOW
                totalRamGiB < 7.0 -> VisualQuality.MID
                else -> VisualQuality.HIGH
            }
            // Older Android versions and few CPU cores point to older hardware, so never start above Mid.
            val ceiling = if (sdk < Build.VERSION_CODES.Q || cpuCores <= 4) VisualQuality.MID else VisualQuality.HIGH
            return if (byRam.ordinal > ceiling.ordinal) ceiling else byRam
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
