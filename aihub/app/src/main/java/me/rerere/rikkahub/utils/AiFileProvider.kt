package me.rerere.rikkahub.utils

import androidx.core.content.FileProvider

/** Its own class so the embedded chat's file sharing does not merge with Absencium's FileProvider declaration. */
class AiFileProvider : FileProvider()
