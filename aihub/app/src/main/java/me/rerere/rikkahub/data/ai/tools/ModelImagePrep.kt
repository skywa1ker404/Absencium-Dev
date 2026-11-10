package me.rerere.rikkahub.data.ai.tools

import android.graphics.Bitmap
import android.graphics.BitmapFactory


internal data class PreparedModelImage(
    val dataUrl: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
)

internal fun prepareImageForModelInspection(
    path: String,
    bytes: ByteArray,
    maxDimension: Int = 1280,
    minDimension: Int = 1,
): PreparedModelImage? {
    if (bytes.size < 12) return null
    val lowerPath = path.lowercase()
    if (lowerPath.endsWith(".svg") || lowerPath.endsWith(".ico") || lowerPath.endsWith(".html") || lowerPath.endsWith(".xml")) {
        return null
    }

    // Validate image magic bytes to ensure raster image format (PNG, JPEG, WEBP, GIF, BMP)
    val isPng = bytes.size >= 8 &&
        bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
    val isJpeg = bytes.size >= 3 &&
        bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
    val isWebp = bytes.size >= 12 &&
        bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
        bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() && bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
    val isGif = bytes.size >= 6 &&
        bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte()
    val isBmp = bytes.size >= 2 &&
        bytes[0] == 'B'.code.toByte() && bytes[1] == 'M'.code.toByte()

    if (!isPng && !isJpeg && !isWebp && !isGif && !isBmp) {
        return null
    }

    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    try {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
    } catch (e: Throwable) {
        return null
    }
    val origWidth = boundsOptions.outWidth
    val origHeight = boundsOptions.outHeight

    if (origWidth <= 0 || origHeight <= 0) {
        return null
    }
    if (origWidth < minDimension || origHeight < minDimension) {
        return null
    }

    var sampleSize = 1
    while (origWidth / (sampleSize * 2) >= maxDimension || origHeight / (sampleSize * 2) >= maxDimension) {
        sampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val decodedBitmap = try {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
    } catch (e: Throwable) {
        null
    } ?: return null

    val finalBitmap = try {
        val width = decodedBitmap.width
        val height = decodedBitmap.height
        if (width > maxDimension || height > maxDimension) {
            val ratio = minOf(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
            val targetWidth = (width * ratio).toInt().coerceAtLeast(1)
            val targetHeight = (height * ratio).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decodedBitmap, targetWidth, targetHeight, true)
        } else {
            decodedBitmap
        }
    } catch (e: Throwable) {
        decodedBitmap
    }

    val finalWidth = finalBitmap.width
    val finalHeight = finalBitmap.height
    val hasAlpha = finalBitmap.hasAlpha()
    val format = if (hasAlpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
    val mime = if (hasAlpha) "image/png" else "image/jpeg"
    val quality = if (hasAlpha) 100 else 85

    val out = java.io.ByteArrayOutputStream()
    finalBitmap.compress(format, quality, out)
    if (finalBitmap !== decodedBitmap) {
        finalBitmap.recycle()
    }
    decodedBitmap.recycle()

    val compressedBytes = out.toByteArray()
    if (compressedBytes.isEmpty()) return null
    val base64 = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
    return PreparedModelImage(
        dataUrl = "data:$mime;base64,$base64",
        mimeType = mime,
        width = finalWidth,
        height = finalHeight,
    )
}

