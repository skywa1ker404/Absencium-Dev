package me.rerere.rikkahub.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import me.rerere.common.platform.PlatformHttpClient
import me.rerere.common.platform.PlatformHttpRequest
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Avatar
import me.rerere.rikkahub.ui.activity.ShortcutHandlerActivity
import org.koin.core.context.GlobalContext
import kotlin.uuid.Uuid

private const val TAG = "AppShortcutManager"

/**
 * Manages dynamic app shortcuts for recently used assistants.
 * These shortcuts appear when long-pressing the app icon.
 */
class AppShortcutManager(
    private val context: Context,
) {
    private val shortcutManager: ShortcutManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            context.getSystemService(ShortcutManager::class.java)
        } else null
    }

    /**
     * Updates the dynamic shortcuts to show the recently used assistants.
     * 
     * @param recentlyUsedIds List of assistant IDs in order of most recent use
     * @param assistants List of all available assistants
     */
    suspend fun updateAssistantShortcuts(
        recentlyUsedIds: List<Uuid>,
        assistants: List<Assistant>
    ) {
        Log.d(TAG, "updateAssistantShortcuts called with ${recentlyUsedIds.size} recent IDs")
        Log.d(TAG, "recentlyUsedIds: $recentlyUsedIds")
        
        val manager = shortcutManager ?: run {
            Log.w(TAG, "ShortcutManager is null (API level too low?)")
            return
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            Log.w(TAG, "API level too low for shortcuts")
            return
        }

        val shortcuts = recentlyUsedIds
            .take(3)
            .mapNotNull { id -> assistants.find { it.id == id } }
            .mapIndexed { index, assistant ->
                Log.d(TAG, "Creating shortcut for assistant: ${assistant.name} (${assistant.id})")
                val intent = Intent(context, ShortcutHandlerActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = "aihub://assistant/${assistant.id}".toUri()
                }
                
                val name = assistant.name.ifEmpty { "Assistant" }
                val icon = createAvatarIcon(assistant.avatar, name)
                
                ShortcutInfo.Builder(context, "assistant_${assistant.id}")
                    .setShortLabel(name)
                    .setLongLabel(context.getString(R.string.shortcut_chat_with, name))
                    .setIcon(icon)
                    .setIntent(intent)
                    .setRank(index)
                    .build()
            }

        Log.d(TAG, "Created ${shortcuts.size} shortcuts")
        
        try {
            manager.dynamicShortcuts = shortcuts
            Log.d(TAG, "Successfully set dynamic shortcuts")
        } catch (e: Exception) {
            // Ignore errors, shortcuts are not critical
            Log.e(TAG, "Error setting shortcuts", e)
            e.printStackTrace()
        }
    }

    /**
     * Creates an Icon from the assistant's avatar.
     */
    private suspend fun createAvatarIcon(avatar: Avatar, fallbackName: String): Icon {
        return when (avatar) {
            is Avatar.Emoji -> {
                // Create a bitmap with the emoji
                createEmojiIcon(avatar.content)
            }
            is Avatar.Image -> {
                // Try to load image from URL, fallback to default
                try {
                    loadImageIcon(avatar.url) ?: createTextIcon(fallbackName)
                } catch (e: Exception) {
                    createTextIcon(fallbackName)
                }
            }
            is Avatar.Resource -> {
                // Use the resource directly without circle cropping
                // This preserves the original icon shape (like the Absencium AI logo)
                Icon.createWithResource(context, avatar.id)
            }
            is Avatar.Dummy -> {
                // Create icon with first letter of name
                createTextIcon(fallbackName)
            }
        }
    }
    


    /**
     * Creates an icon with an emoji.
     */
    private fun createEmojiIcon(emoji: String): Icon {
        val size = 108 // Adaptive icon size
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        
        // Draw background
        val bgPaint = Paint().apply {
            color = 0xFFE8E8E8.toInt()
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)
        
        // Draw emoji
        val textPaint = Paint().apply {
            textSize = size * 0.5f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val bounds = Rect()
        textPaint.getTextBounds(emoji, 0, emoji.length, bounds)
        val y = size / 2f + bounds.height() / 2f
        canvas.drawText(emoji, size / 2f, y, textPaint)
        
        return Icon.createWithAdaptiveBitmap(bitmap)
    }

    /**
     * Creates an icon with the first letter of the name.
     */
    private fun createTextIcon(name: String): Icon {
        val size = 108
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        
        // Draw background
        val bgPaint = Paint().apply {
            color = 0xFF6750A4.toInt() // Material You primary color
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)
        
        // Draw letter
        val letter = name.firstOrNull()?.uppercase() ?: "A"
        val textPaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = size * 0.45f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val bounds = Rect()
        textPaint.getTextBounds(letter, 0, letter.length, bounds)
        val y = size / 2f + bounds.height() / 2f
        canvas.drawText(letter, size / 2f, y, textPaint)
        
        return Icon.createWithAdaptiveBitmap(bitmap)
    }

    /**
     * Loads an avatar image and creates a circular adaptive icon.
     *
     * Assistant/user avatars are stored as local URIs (`file://` or `content://`) far more
     * often than as remote URLs, so those must be resolved through the ContentResolver.
     * Only http(s) URLs are fetched over the network.
     */
    private suspend fun loadImageIcon(url: String): Icon? = withContext(Dispatchers.IO) {
        try {
            val body = loadAvatarBytes(url) ?: return@withContext null
            decodeCircularAdaptiveIcon(body)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun loadAvatarBytes(url: String): ByteArray? {
        val uri = url.toUri()
        return when (uri.scheme?.lowercase()) {
            "http", "https" -> {
                val response = withTimeout(5_000L) {
                    GlobalContext.get().get<PlatformHttpClient>().execute(
                        PlatformHttpRequest(
                            method = "GET",
                            url = url,
                        )
                    )
                }
                if (response.statusCode != 200) null else response.body
            }
            // file://, content://, android.resource:// — the common case for saved avatars
            else -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }
    }

    private fun decodeCircularAdaptiveIcon(body: ByteArray): Icon? {
        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(body, 0, body.size, options)
        options.inJustDecodeBounds = false
        options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, 108, 108)
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(body, 0, body.size, options)
            ?: return null

        // Create circular adaptive icon
        val size = 108
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val circularBitmap = createBitmap(size, size)
        val canvas = Canvas(circularBitmap)

        val paint = Paint().apply {
            isAntiAlias = true
            shader = android.graphics.BitmapShader(
                scaledBitmap,
                android.graphics.Shader.TileMode.CLAMP,
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        bitmap.recycle()

        return Icon.createWithAdaptiveBitmap(circularBitmap)
    }


    private fun calculateInSampleSize(srcWidth: Int, srcHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (srcHeight > reqHeight || srcWidth > reqWidth) {
            var halfHeight = srcHeight / 2
            var halfWidth = srcWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
