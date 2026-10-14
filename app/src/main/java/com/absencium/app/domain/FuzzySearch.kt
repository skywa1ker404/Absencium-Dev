package com.absencium.app.domain

import java.util.Locale

/**
 * Forgiving text matching for the global search. Every word of the query must match somewhere in
 * the text. A word matches as a plain substring (best), as a close spelling of a word in the text
 * (one typo), or as letters appearing in order inside a word (loosest).
 */
object FuzzySearch {

    private const val SUBSTRING = 100
    private const val WORD_START_BONUS = 20
    private const val TYPO = 60
    private const val IN_ORDER = 30

    /** Words longer than this are not checked for typos; it keeps long notes fast. */
    private const val MAX_WORDS_CHECKED = 3000

    private val splitter = Regex("[^\\p{L}\\p{N}]+")

    private fun words(text: String): List<String> =
        text.lowercase(Locale.getDefault()).split(splitter).filter { it.isNotEmpty() }

    /** Higher is a better match; 0 means the text does not match the query. */
    fun score(query: String, text: String): Int {
        val queryWords = words(query)
        if (queryWords.isEmpty() || text.isBlank()) return 0
        val lower = text.lowercase(Locale.getDefault())
        val textWords by lazy { words(text).take(MAX_WORDS_CHECKED) }
        var total = 0
        for (word in queryWords) {
            val at = lower.indexOf(word)
            val wordScore = when {
                at >= 0 -> SUBSTRING + if (at == 0 || !lower[at - 1].isLetterOrDigit()) WORD_START_BONUS else 0
                word.length >= 4 && textWords.any { withinOneEdit(word, it) } -> TYPO
                word.length >= 3 && textWords.any { inOrder(word, it) } -> IN_ORDER
                else -> 0
            }
            if (wordScore == 0) return 0
            total += wordScore
        }
        return total / queryWords.size
    }

    /** True when [a] and [b] differ by at most one inserted, removed or changed letter. */
    internal fun withinOneEdit(a: String, b: String): Boolean {
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        var i = 0
        var j = 0
        var edits = 0
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) {
                i++
                j++
                continue
            }
            if (++edits > 1) return false
            when {
                a.length > b.length -> i++
                a.length < b.length -> j++
                else -> {
                    i++
                    j++
                }
            }
        }
        return edits + (a.length - i) + (b.length - j) <= 1
    }

    /** True when every letter of [needle] appears in [haystack] in the same order. */
    internal fun inOrder(needle: String, haystack: String): Boolean {
        if (needle.length > haystack.length) return false
        var n = 0
        for (c in haystack) {
            if (c == needle[n]) n++
            if (n == needle.length) return true
        }
        return false
    }
}
