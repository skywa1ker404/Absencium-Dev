package skywa1ker404.absencium.logging

import android.view.Choreographer

/**
 * Writes a line to debug.log every few seconds in which frames were slow, with how many and the worst. It exists so lag
 * reports come with numbers: after scrolling a laggy screen, the log says whether frames really were missed and how badly.
 * It does nothing while the app is in the background and costs one cheap callback per frame while it is open.
 */
object FrameMonitor : Choreographer.FrameCallback {
    private const val SLOW_NANOS = 34_000_000L // about two frames at 60 Hz
    private const val REPORT_NANOS = 3_000_000_000L

    private var running = false
    private var last = 0L
    private var windowStart = 0L
    private var frames = 0
    private var slow = 0
    private var worstNanos = 0L

    fun start() {
        if (running) return
        running = true
        last = 0L
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stop() {
        running = false
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        if (last != 0L) {
            val delta = frameTimeNanos - last
            // A gap over a second is the app being idle or paused, not a slow frame.
            if (delta < 1_000_000_000L) {
                frames++
                if (delta > SLOW_NANOS) {
                    slow++
                    if (delta > worstNanos) worstNanos = delta
                }
            }
        }
        last = frameTimeNanos
        if (windowStart == 0L) windowStart = frameTimeNanos
        if (frameTimeNanos - windowStart >= REPORT_NANOS) {
            if (slow > 0) {
                DebugLogger.i("Frames", "$slow slow frame(s) of $frames in the last 3 s, worst ${worstNanos / 1_000_000} ms")
            }
            windowStart = frameTimeNanos
            frames = 0
            slow = 0
            worstNanos = 0L
        }
        Choreographer.getInstance().postFrameCallback(this)
    }
}
