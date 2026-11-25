package me.rerere.rikkahub.utils

import kotlin.text.Regex

private val REGEX_CODE_BLOCK = Regex("```[\\s\\S]*?```|`[^`]*?`")
private val REGEX_IMAGE_LINK = Regex("!?\\[([^\\]]+)\\]\\([^\\)]*\\)")
private val REGEX_BOLD = Regex("\\*\\*([^*]+?)\\*\\*")
private val REGEX_ITALIC = Regex("\\*([^*]+?)\\*")
private val REGEX_UNDERLINE_DOUBLE = Regex("__([^_]+?)__")
private val REGEX_UNDERLINE_SINGLE = Regex("_([^_]+?)_")
private val REGEX_STRIKETHROUGH = Regex("~~([^~]+?)~~")
private val REGEX_HIGHLIGHT = Regex("==([^=]+?)==|<mark>(.*?)</mark>")
private val REGEX_UNDERLINE_PLUS = Regex("\\+\\+([^+]+?)\\+\\+")
private val REGEX_HTML_FORMATTING_TAG = Regex("(?i)</?(?:u|ins|mark|b|strong|i|em|s|del|strike|sub|sup|small|code|span|font)(?:\\s+[^>]*)?>")
private val REGEX_HEADING = Regex("(?m)^#+\\s*")
private val REGEX_LIST_BULLET = Regex("(?m)^\\s*[-*+]\\s+")
private val REGEX_LIST_NUMBERED = Regex("(?m)^\\s*\\d+\\.\\s+")
private val REGEX_BLOCKQUOTE = Regex("(?m)^>\\s*")
private val REGEX_HORIZONTAL_RULE = Regex("(?m)^(\\s*[-*_]){3,}\\s*$")
private val REGEX_MULTIPLE_NEWLINES = Regex("\n{3,}")


