package skywa1ker404.absencium.ui.vault.entry

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.ui.icons.AppIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes photos from the app's storage at a size that suits the screen, honouring the EXIF rotation. */
object ImageTools {

    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    /** Returns the photo scaled so its longest side is at least [maxEdge] pixels (never upscaled), or null if it cannot be read. */
    fun decode(path: String, maxEdge: Int): Bitmap? {
        val key = "$path@$maxEdge"
        cache.get(key)?.let { return it }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null
        var sample = 1
        while (longest / (sample * 2) >= maxEdge) sample *= 2
        val raw = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val degrees = rotationDegrees(path)
        val result = if (degrees == 0) {
            raw
        } else {
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
        }
        cache.put(key, result)
        return result
    }

    /** Forgets every cached size of a photo, after the file was changed. */
    fun forget(path: String) {
        cache.snapshot().keys.filter { it.startsWith("$path@") }.forEach { cache.remove(it) }
    }

    private fun rotationDegrees(path: String): Int =
        try {
            when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
}

/** Loads a photo off the main thread. [version] changes when the file is edited so the picture reloads. */
@Composable
fun rememberPhoto(path: String, maxEdge: Int, version: Int = 0): State<ImageBitmap?> =
    produceState<ImageBitmap?>(initialValue = null, path, maxEdge, version) {
        value = withContext(Dispatchers.IO) { ImageTools.decode(path, maxEdge)?.asImageBitmap() }
    }

/** A squircle thumbnail. When [onRemove] is given, a small round X in the top right corner discards the photo. */
@Composable
fun PhotoThumb(
    path: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null,
    size: Dp = 92.dp,
    version: Int = 0,
) {
    val colors = MaterialTheme.colorScheme
    val bitmap by rememberPhoto(path, 360, version)
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(size * 0.3f))
                .background(colors.surfaceContainerHighest)
                .clickable(onClick = onClick),
        ) {
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = "Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.62f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Close, contentDescription = "Discard photo", tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }
    }
}
