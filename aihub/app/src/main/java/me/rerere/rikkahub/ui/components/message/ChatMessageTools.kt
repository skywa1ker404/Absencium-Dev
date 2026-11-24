package me.rerere.rikkahub.ui.components.message

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import me.rerere.rikkahub.ui.components.ui.CircularProgressIndicator
import me.rerere.rikkahub.ui.components.dots.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.highlight.HighlightText
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.repository.MemoryRepository
import me.rerere.rikkahub.ui.components.richtext.HighlightCodeBlock
import me.rerere.rikkahub.ui.components.richtext.MarkdownBlock
import me.rerere.rikkahub.ui.components.richtext.ZoomableAsyncImage
import me.rerere.rikkahub.ui.theme.AppShapes
import me.rerere.rikkahub.ui.components.ui.Favicon
import me.rerere.rikkahub.ui.components.ui.FaviconRow
import me.rerere.rikkahub.ui.components.ui.FormItem
import me.rerere.rikkahub.ui.components.ui.AiDestructiveConfirmDialog
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.modifier.aiSheetContainerColor
import me.rerere.rikkahub.ui.modifier.shimmer
import me.rerere.rikkahub.utils.JsonInstantPretty
import me.rerere.rikkahub.utils.jsonPrimitiveOrNull
import me.rerere.rikkahub.utils.saveToDownloads
import org.koin.compose.koinInject


@Composable
private fun ToolCallPreviewSheet(
    toolName: String,
    arguments: JsonElement,
    content: JsonElement,
    onDismissRequest: () -> Unit = {}
) {
    val navController = LocalNavController.current
    val memoryRepo: MemoryRepository = koinInject()
    val scope = rememberCoroutineScope()
    var pendingMemoryDeleteId by remember { mutableStateOf<Int?>(null) }

    // Check if this is a memory creation/update operation
    val isMemoryOperation = toolName in listOf("create_memory", "edit_memory")
    val memoryId = (content as? JsonObject)?.get("id")?.jsonPrimitiveOrNull?.intOrNull

    ModalBottomSheet(
        containerColor = aiSheetContainerColor(),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = {
            onDismissRequest()
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .padding(16.dp)
                    .padding(16.dp),
                    //.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (toolName) {
                    "search_web" -> {
                        Text(
                            stringResource(
                                R.string.chat_message_tool_search_prefix,
                                (arguments as? JsonObject)?.get("query")?.jsonPrimitiveOrNull?.contentOrNull ?: ""
                            )
                        )
                        val contentObj = content as? JsonObject
                        val items = contentObj?.get("items")?.jsonArray ?: emptyList()
                        val images = contentObj?.get("images")?.jsonArray ?: emptyList()
                        val answer = contentObj?.get("answer")?.jsonPrimitive?.contentOrNull
                        if (items.isNotEmpty() || images.isNotEmpty() || !answer.isNullOrBlank()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (answer != null) {
                                    item {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        ) {
                                            MarkdownBlock(
                                                content = answer,
                                                modifier = Modifier
                                                    .padding(16.dp)
                                                    .fillMaxWidth(),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }

                                items(items.size, key = { index -> "search_item_$index" }) { index ->
                                    val it = items[index]
                                    val itemObj = it as? JsonObject ?: return@items
                                    val url = itemObj["url"]?.jsonPrimitive?.content ?: return@items
                                    val title =
                                        itemObj["title"]?.jsonPrimitive?.content
                                            ?: return@items
                                    val text = itemObj["text"]?.jsonPrimitive?.content ?: return@items
                                    Card(
                                        onClick = {
                                            navController.navigate(Screen.WebView(url = url))
                                        },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp, horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Favicon(
                                                url = url,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = title,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = text,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    text = url,
                                                    maxLines = 1,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.6f
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                if (images.isNotEmpty()) {
                                    item {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(images.size, key = { index -> "search_image_$index" }) { index ->
                                                val imageObj = images[index] as? JsonObject ?: return@items
                                                val imageUrl = imageObj["url"]
                                                    ?.jsonPrimitiveOrNull?.contentOrNull ?: return@items
                                                val imageTitle = imageObj["title"]
                                                    ?.jsonPrimitiveOrNull?.contentOrNull.orEmpty()
                                                Card(
                                                    shape = AppShapes.CardSmall,
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                                    )
                                                ) {
                                                    Column(
                                                        modifier = Modifier.size(width = 160.dp, height = 160.dp)
                                                    ) {
                                                        ZoomableAsyncImage(
                                                            model = imageUrl,
                                                            contentDescription = imageTitle.ifBlank { null },
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .fillMaxWidth()
                                                                .clip(AppShapes.CardSmall),
                                                            contentScale = ContentScale.Crop,
                                                        )
                                                        if (imageTitle.isNotBlank()) {
                                                            Text(
                                                                text = imageTitle,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                modifier = Modifier.padding(
                                                                    horizontal = 8.dp,
                                                                    vertical = 6.dp
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            HighlightText(
                                code = JsonInstantPretty.encodeToString(content),
                                language = "json",
                                fontSize = 12.sp
                            )
                        }
                    }

                    "scrape_web" -> {
                        val urls = (content as? JsonObject)?.get("urls")?.jsonArray ?: emptyList()
                        Text(
                            text = stringResource(
                                R.string.chat_message_tool_scrape_prefix,
                                urls.joinToString(", ") { (it as? JsonObject)?.get("url")?.jsonPrimitiveOrNull?.contentOrNull ?: "" }),
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(urls.size, key = { index -> "scrape_url_$index" }) { index ->
                                val url = urls[index]
                                val urlObject = url as? JsonObject
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = urlObject?.get("url")?.jsonPrimitive?.content ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Card {
                                        MarkdownBlock(
                                            content = urlObject?.get("content")?.jsonPrimitive?.content ?: "",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    8.dp
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Python tools - show code with syntax highlighting and formatted output
                    "eval_python" -> {
                        val code = (arguments as? JsonObject)?.get("code")?.jsonPrimitiveOrNull?.contentOrNull ?: ""
                        val contentObj = content as? JsonObject
                        val result = contentObj?.get("result")?.jsonPrimitiveOrNull?.contentOrNull
                        val stdout = contentObj?.get("stdout")?.jsonPrimitiveOrNull?.contentOrNull
                        val stderr = contentObj?.get("stderr")?.jsonPrimitiveOrNull?.contentOrNull
                        val error = contentObj?.get("error")?.jsonPrimitiveOrNull?.contentOrNull
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Code section
                            Text(
                                text = stringResource(R.string.chat_message_tool_python_code),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                HighlightText(
                                    code = code,
                                    language = "python",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            
                            // Output section
                            Text(
                                text = stringResource(R.string.chat_message_tool_python_output),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (error != null) 
                                        MaterialTheme.colorScheme.errorContainer 
                                    else MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (error != null) {
                                        Text(
                                            text = error,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    } else {
                                        if (!stdout.isNullOrBlank()) {
                                            Text(
                                                text = stdout,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        }
                                        if (!result.isNullOrBlank() && result != "null") {
                                            Text(
                                                text = "→ $result",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        }
                                        if (!stderr.isNullOrBlank()) {
                                            Text(
                                                text = stderr,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        if (stdout.isNullOrBlank() && (result.isNullOrBlank() || result == "null") && stderr.isNullOrBlank()) {
                                            Text(
                                                text = "(no output)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // File write tool - show file info with download button
                    "write_sandbox_file" -> {
                        val path = (arguments as? JsonObject)?.get("path")?.jsonPrimitiveOrNull?.contentOrNull ?: ""
                        val contentObj = content as? JsonObject
                        val success = contentObj?.get("success")?.jsonPrimitive?.content?.toBooleanStrictOrNull() == true
                        val uri = contentObj?.get("uri")?.jsonPrimitiveOrNull?.contentOrNull
                        val error = contentObj?.get("error")?.jsonPrimitiveOrNull?.contentOrNull
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val scope = rememberCoroutineScope()
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.chat_message_tool_file_created),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (success) 
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // File path
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (success) Icons.Rounded.Build
                                                else Icons.Rounded.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = if (success) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = path,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    
                                    if (error != null) {
                                        Text(
                                            text = error,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    
                                    // Download button
                                    if (success && uri != null) {
                                        androidx.compose.material3.FilledTonalButton(
                                            onClick = {
                                                val uriParsed = android.net.Uri.parse(uri)
                                                scope.launch {
                                                    context.saveToDownloads(uriParsed, path.substringAfterLast("/"))
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ContentCopy,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            androidx.compose.foundation.layout.Spacer(
                                                modifier = Modifier.size(8.dp)
                                            )
                                            Text(stringResource(R.string.chat_message_tool_download_file))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.chat_message_tool_call_title),
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )

                            // 如果是memory操作，允许用户快速删除
                            if (isMemoryOperation && memoryId != null) {
                                IconButton(
                                    onClick = { pendingMemoryDeleteId = memoryId }
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = stringResource(R.string.a11y_delete_memory)
                                    )
                                }
                            }
                        }
                        
                        // Arguments section
                        val clipboardManager = LocalClipboardManager.current
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val argumentsJson = remember(arguments) {
                                JsonInstantPretty.encodeToString(arguments)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.chat_message_tool_call_label,
                                        toolName
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    modifier = Modifier.size(24.dp),
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(argumentsJson))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = stringResource(R.string.a11y_copy_arguments),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                HighlightText(
                                    code = argumentsJson,
                                    language = "json",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // Result section
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val contentJson = remember(content) {
                                JsonInstantPretty.encodeToString(content)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.chat_message_tool_call_result),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    modifier = Modifier.size(24.dp),
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(contentJson))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = stringResource(R.string.a11y_copy_result),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                HighlightText(
                                    code = contentJson,
                                    language = "json",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
    )

    pendingMemoryDeleteId?.let { id ->
        AiDestructiveConfirmDialog(
            title = stringResource(R.string.chat_message_delete_memory_title),
            consequence = stringResource(R.string.chat_message_delete_memory_consequence),
            onDismiss = { pendingMemoryDeleteId = null },
            onConfirm = {
                pendingMemoryDeleteId = null
                scope.launch {
                    runCatching {
                        memoryRepo.deleteMemory(id)
                        onDismissRequest()
                    }
                }
            },
        )
    }
}
