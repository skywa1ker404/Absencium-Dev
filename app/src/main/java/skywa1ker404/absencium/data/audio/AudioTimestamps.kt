package skywa1ker404.absencium.data.audio

/** Bookmarks of a recording (millisecond offsets), stored as a JSON array like `[1500,42000]`, and how times are shown. Pure. */
object AudioTimestamps {

    fun encode(offsets: List<Long>): String = offsets.filter { it >= 0 }.sorted().joinToString(prefix = "[", postfix = "]", separator = ",")

    /** Reads what [encode] wrote. Anything that is not a plain list of non-negative numbers gives an empty list, never an error. */
    fun decode(json: String): List<Long> {
        val inner = json.trim().removePrefix("[").removeSuffix("]").trim()
        if (inner.isEmpty()) return emptyList()
        val out = ArrayList<Long>()
        for (part in inner.split(',')) {
            val value = part.trim().toLongOrNull() ?: return emptyList()
            if (value < 0) return emptyList()
            out += value
        }
        return out.sorted()
    }

    /** `0:05`, `12:34`, or `1:02:03` from an hour on. */
    fun format(ms: Long): String {
        val total = (ms.coerceAtLeast(0) / 1000)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
