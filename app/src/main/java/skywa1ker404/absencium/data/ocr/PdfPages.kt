package skywa1ker404.absencium.data.ocr

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

/** Draws the pages of a PDF with Android's own renderer (no library). Pages come out on a white background. */
object PdfPages {

    /** How many pages the file has, or 0 when it cannot be opened. */
    fun count(file: File): Int = try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd -> PdfRenderer(fd).use { it.pageCount } }
    } catch (e: Exception) {
        0
    }

    /**
     * Draws pages `0 until min(pageCount, [maxPages])` at [width] pixels wide and gives each to [onPage] (index, bitmap). The bitmap
     * is only valid inside the call. Returns the number of pages in the file, or 0 when it cannot be opened.
     */
    fun render(file: File, width: Int, maxPages: Int, onPage: (Int, Bitmap) -> Unit): Int {
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    val total = renderer.pageCount
                    for (i in 0 until minOf(total, maxPages)) {
                        renderer.openPage(i).use { page ->
                            val scale = width.toFloat() / page.width.coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(width, (page.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            try {
                                onPage(i, bitmap)
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }
                    total
                }
            }
        } catch (e: java.io.IOException) {
            0
        } catch (e: SecurityException) {
            0
        }
    }
}
