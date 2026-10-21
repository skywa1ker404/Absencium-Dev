package skywa1ker404.absencium.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import skywa1ker404.absencium.logging.DebugLogger
import java.io.File

/** Records the microphone to an AAC (.m4a) file. One recording at a time; the caller must hold the RECORD_AUDIO permission. */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var startedAt = 0L

    val isRecording: Boolean get() = recorder != null

    /** Milliseconds since [start]; 0 when not recording. */
    fun elapsedMs(): Long = if (recorder == null) 0L else SystemClock.elapsedRealtime() - startedAt

    /** Starts writing to [target]. Returns null on success, or a message that says why the microphone could not be used. */
    fun start(target: File): String? {
        if (recorder != null) return "A recording is already running."
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(target.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            startedAt = SystemClock.elapsedRealtime()
            null
        } catch (e: Exception) {
            DebugLogger.e("Audio", "Could not start recording", e)
            runCatching { r.release() }
            target.delete()
            "The microphone could not be started. Another app may be using it."
        }
    }

    /** Stops and returns the length in milliseconds, or null when the recording was too short or failed (the file is removed). */
    fun stop(target: File): Long? {
        val r = recorder ?: return null
        val length = elapsedMs()
        recorder = null
        return try {
            r.stop()
            r.release()
            length
        } catch (e: RuntimeException) {
            // MediaRecorder.stop throws when nothing was recorded yet.
            DebugLogger.w("Audio", "Recording was too short to keep")
            runCatching { r.release() }
            target.delete()
            null
        }
    }

    /** Throws the running recording away (screen closed, app stopped). */
    fun abort(target: File?) {
        val r = recorder ?: return
        recorder = null
        runCatching { r.stop() }
        runCatching { r.release() }
        target?.delete()
    }
}
