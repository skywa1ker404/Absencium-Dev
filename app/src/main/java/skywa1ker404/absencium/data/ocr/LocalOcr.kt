package skywa1ker404.absencium.data.ocr

import android.content.Context
import android.graphics.Bitmap
import cz.adaptech.tesseract4android.TessBaseAPI
import skywa1ker404.absencium.logging.DebugLogger
import java.io.File
import java.io.IOException

/**
 * Reads text from a picture on the phone itself with Tesseract (no network, no AI). The English language file ships inside the app
 * and is copied to private storage the first time it is needed. One reading at a time; the engine is not thread-safe.
 */
class LocalOcr(private val context: Context) {

    private val lock = Any()

    /** The folder that holds `tessdata/eng.traineddata`, copied from the app's assets when missing. */
    private fun dataRoot(): File {
        val root = File(context.filesDir, "tesseract")
        val dir = File(root, "tessdata")
        if (!dir.exists() && !dir.mkdirs()) throw IOException("Could not prepare the text reader's storage.")
        val target = File(dir, LANGUAGE_FILE)
        if (!target.isFile || target.length() == 0L) {
            val part = File(dir, "$LANGUAGE_FILE.part")
            context.assets.open("tessdata/$LANGUAGE_FILE").use { input -> part.outputStream().use { input.copyTo(it) } }
            if (!part.renameTo(target)) throw IOException("Could not prepare the text reader's language file.")
        }
        return root
    }

    /** The text in [bitmap], top to bottom. Empty when none was found. Takes a moment, so call it off the main thread. */
    fun read(bitmap: Bitmap): String = synchronized(lock) {
        val root = dataRoot()
        val api = TessBaseAPI()
        try {
            if (!api.init(root.absolutePath, LANGUAGE)) throw IOException("The text reader could not start.")
            api.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
            api.setImage(bitmap)
            api.getUTF8Text().orEmpty().trim()
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            DebugLogger.e("Ocr", "Reading a picture failed", e)
            throw IOException("The text reader failed: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            api.recycle()
        }
    }

    private companion object {
        const val LANGUAGE = "eng"
        const val LANGUAGE_FILE = "eng.traineddata"
    }
}
