package me.rerere.rikkahub.utils

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.AssistantMemory
import me.rerere.rikkahub.data.model.Avatar
import me.rerere.rikkahub.data.repository.MemoryRepository
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.db.dao.ChatEpisodeDAO
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.uuid.Uuid

@Serializable
data class AssistantExportV1(
    val version: Int = 1,
    val format: String = "aihub_assistant",
    val assistant: Assistant,
    // Bundled assets
    val avatarContent: String? = null, // Base64 encoded avatar image
    val avatarMimeType: String? = null,
    val backgroundContent: String? = null, // Base64 encoded chat background image
    val backgroundMimeType: String? = null,
    // Bundled Memories
    val memories: List<AssistantMemory> = emptyList(),
)

object AssistantExportImport : KoinComponent {
    private val settingsStore: SettingsStore by inject()
    private val memoryRepository: MemoryRepository by inject()
    private val chatEpisodeDAO: ChatEpisodeDAO by inject()

    // Unknown keys are ignored so older bundles that still carry removed sections import fine.
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        encodeDefaults = true
    }

    /** Export an Assistant to Absencium AI Bundle JSON format (settings, avatar, background, optional memories). */
    suspend fun exportToAiBundle(
        assistant: Assistant,
        context: Context,
        includeMemories: Boolean
    ): String {
        // 1. Process Avatar
        var avatarContent: String? = null
        var avatarMime: String? = null
        if (assistant.avatar is Avatar.Image) {
            val url = (assistant.avatar as Avatar.Image).url
            try {
                val bytes = readUriBytes(context, url)
                if (bytes != null) {
                    avatarContent = base64Encode(bytes)
                    avatarMime = "image/*" // Simplified, can detect if needed
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Process chat background. The Assistant field itself only contains a
        // local URI, so bundle the bytes as well to keep the background portable.
        var backgroundContent: String? = null
        var backgroundMime: String? = null
        assistant.background?.let { url ->
            try {
                val bytes = readUriBytes(context, url)
                if (bytes != null) {
                    backgroundContent = base64Encode(bytes)
                    backgroundMime = context.getFileMimeType(Uri.parse(url)) ?: "image/*"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Process Memories
        val bundledMemories: List<AssistantMemory> = if (includeMemories) {
            // Fetch Core Memories (already returns AssistantMemory list)
            val coreConfigured = memoryRepository.getMemoriesOfAssistant(assistant.id.toString())
            
            // Fetch Episodic Memories
            val episodes = memoryRepository.getEpisodeEntitiesOfAssistant(assistant.id.toString())
            val episodicConfigured = episodes.map {
                AssistantMemory(
                    id = -it.id, // Negative to distinguish
                    content = it.content,
                    type = 1, // EPISODIC
                    hasEmbedding = !it.embedding.isNullOrBlank() || it.embeddingBlob != null,
                    embeddingModelId = it.embeddingModelId,
                    timestamp = it.startTime,
                    significance = it.significance
                )
            }
            
            coreConfigured + episodicConfigured
        } else {
            emptyList()
        }

        val export = AssistantExportV1(
            assistant = assistant,
            avatarContent = avatarContent,
            avatarMimeType = avatarMime,
            backgroundContent = backgroundContent,
            backgroundMimeType = backgroundMime,
            memories = bundledMemories,
        )

        return json.encodeToString(AssistantExportV1.serializer(), export)
    }

    /** Import an Assistant from Absencium AI Bundle JSON format. */
    suspend fun importFromAiBundle(jsonContent: String, context: Context): Assistant {
        val export = json.decodeFromString<AssistantExportV1>(jsonContent)
        return finalizeAiImport(export, context, true)
    }

    /** Finalize the import from a parsed ExportV1 object. Restores files and, when asked, memories. */
    suspend fun finalizeAiImport(
        export: AssistantExportV1,
        context: Context,
        importMemories: Boolean
    ): Assistant {
        var assistant = export.assistant.copy(id = Uuid.random()) // New Import = New ID

        // 1. Restore Avatar
        if (export.avatarContent != null && assistant.avatar is Avatar.Image) {
            val fileName = "avatar_${assistant.id}_${System.currentTimeMillis()}.png" // assume png or use mime
            val file = File(context.filesDir, "avatars/$fileName") 
            file.parentFile?.mkdirs()
            try {
                val bytes = base64Decode(export.avatarContent)
                file.writeBytes(bytes)
                assistant = assistant.copy(avatar = Avatar.Image(url = Uri.fromFile(file).toString()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        // 2. Restore chat background to an app-owned file. Older bundles do not
        // contain backgroundContent and continue to import without modification.
        if (export.backgroundContent != null) {
            val extension = export.backgroundMimeType
                ?.substringBefore(';')
                ?.trim()
                ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                ?.takeIf { it.isNotBlank() }
                ?: "img"
            val fileName = "background_${assistant.id}_${System.currentTimeMillis()}.$extension"
            val file = context.getOwnedDirectory(OwnedFileDirectory.ASSISTANT_BACKGROUND)
                .resolve(fileName)
            try {
                file.writeBytes(base64Decode(export.backgroundContent))
                assistant = assistant.copy(background = Uri.fromFile(file).toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Restore Memories
        if (importMemories) {
            export.memories.forEach { memory ->
                if (memory.type == 0) { // Core
                     memoryRepository.addMemory(
                         assistantId = assistant.id.toString(),
                         content = memory.content
                     )
                } else if (memory.type == 1) { // Episodic
                     val entity = me.rerere.rikkahub.data.db.entity.ChatEpisodeEntity(
                         id = 0, // Auto-generate
                         assistantId = assistant.id.toString(),
                         startTime = memory.timestamp,
                         endTime = memory.timestamp, // approximate
                         content = memory.content,
                         lastAccessedAt = System.currentTimeMillis(),
                         significance = memory.significance ?: 5,
                         embedding = null, // Needs regeneration
                         embeddingModelId = null
                     )
                     chatEpisodeDAO.insertEpisode(entity)
                }
            }
        }

        return assistant
    }

    private fun readUriBytes(context: Context, url: String): ByteArray? {
        val uri = Uri.parse(url)
        return when {
            url.startsWith("file://") -> {
                val file = File(uri.path ?: return null)
                if (file.exists()) file.readBytes() else null
            }
            url.startsWith("content://") -> {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            url.startsWith("/") -> {
                // Plain file path without scheme
                val file = File(url)
                if (file.exists()) file.readBytes() else null
            }
            url.startsWith("http://") || url.startsWith("https://") -> {
                // Network URL - skip for now (would need async loading)
                null
            }
            else -> null
        }
    }

    fun getSuggestedFileName(assistant: Assistant, format: String): String {
        val baseName = assistant.name.ifEmpty { "assistant" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(50)
        return "${baseName}_bundle.json"
    }

    // -- Import Logic with Config --

    @Serializable
    sealed class ImportResult {
        data class Success(val assistant: Assistant) : ImportResult()

        data class Configurable(
            val assistant: Assistant,
            val exportV1: AssistantExportV1?,
            val hasMemories: Boolean,
            val missingModels: List<String> // List of missing model IDs (names for display)
        ) : ImportResult()

        data class Error(val message: String) : ImportResult()
    }

    /** Parses an Absencium AI bundle (JSON). Returns a Configurable result, or Error. */
    suspend fun parseImport(uri: Uri, context: Context): ImportResult {
        return try {
            val contentBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return ImportResult.Error("Failed to read file")

            val text = String(contentBytes, Charsets.UTF_8).removePrefix("\uFEFF").trim()
            if (!text.contains("\"format\": \"aihub_assistant\"") && !text.contains("\"format\":\"aihub_assistant\"")) {
                return ImportResult.Error("Unsupported file: expected an Absencium AI assistant bundle")
            }
            val export = try {
                json.decodeFromString<AssistantExportV1>(text)
            } catch (e: Exception) {
                return ImportResult.Error("JSON Parse Error: ${e.message ?: ""}")
            }
            ImportResult.Configurable(
                assistant = export.assistant,
                exportV1 = export,
                hasMemories = export.memories.isNotEmpty(),
                missingModels = checkMissingModels(export.assistant)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult.Error(e.message ?: "Unknown Parsing Error")
        }
    }

    private fun checkMissingModels(assistant: Assistant): List<String> {
        val settings = settingsStore.settingsFlow.value
        val missing = mutableListOf<String>()
        
        fun check(id: Uuid?, name: String) {
            if (id != null) {
                 val exists = settings.findModelById(id) != null
                 if (!exists) {
                     missing.add(name)
                 }
            }
        }
        
        check(assistant.chatModelId, "Chat Model")
        check(assistant.backgroundModelId, "Background Model")
        check(assistant.embeddingModelId, "Embedding Model")
        return missing
    }
    
    // Function to clear missing models from assistant
    fun clearMissingModels(assistant: Assistant): Assistant {
        val settings = settingsStore.settingsFlow.value
        
        fun checkAndClear(id: Uuid?): Uuid? {
             if (id != null) {
                 val exists = settings.findModelById(id) != null
                 return if (exists) id else null
             }
             return null
        }
        
        return assistant.copy(
            chatModelId = checkAndClear(assistant.chatModelId),
            backgroundModelId = checkAndClear(assistant.backgroundModelId),
            embeddingModelId = checkAndClear(assistant.embeddingModelId),
            summarizerModelId = null
        )
    }
}

@OptIn(ExperimentalEncodingApi::class)
private fun base64Encode(bytes: ByteArray): String = Base64.encode(bytes)

@OptIn(ExperimentalEncodingApi::class)
private fun base64Decode(value: String): ByteArray {
    return decodeBase64OrNull(value) ?: Base64.decode(value)
}

@OptIn(ExperimentalEncodingApi::class)
internal fun decodeBase64OrNull(value: String): ByteArray? {
    try {
        val result = android.util.Base64.decode(value, android.util.Base64.DEFAULT)
        if (result != null && result.isNotEmpty()) return result
    } catch (e: Throwable) {}

    val cleaned = value.replace("\r", "").replace("\n", "").replace(" ", "").trim()
    val missingPadding = (4 - (cleaned.length % 4)) % 4
    val padded = if (missingPadding in 1..3) cleaned + "=".repeat(missingPadding) else cleaned

    try {
        return Base64.Mime.decode(padded)
    } catch (e: Throwable) {}

    try {
        return Base64.Default.decode(padded)
    } catch (e: Throwable) {}

    return null
}


