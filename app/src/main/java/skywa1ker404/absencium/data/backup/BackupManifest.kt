package skywa1ker404.absencium.data.backup

import java.io.InputStream
import java.security.MessageDigest

/** One file inside a backup package, as the manifest describes it. [sha256] is lower-case hex. */
data class ManifestFile(val path: String, val size: Long, val sha256: String)

/**
 * The table of contents of a backup package. It is the last entry of the zip, so it can carry the size and SHA-256 of
 * every other entry. Restoring checks all of them before anything is staged.
 */
data class BackupManifest(
    val formatVersion: Int,
    val appVersion: String,
    val schemaVersion: Int,
    /** Epoch milliseconds. */
    val createdAt: Long,
    val includesAttachments: Boolean,
    val encrypted: Boolean,
    val counts: Map<String, Int>,
    val files: List<ManifestFile>,
)

/** The rules that decide whether a package can be trusted. Pure functions, so they are unit-tested without Android. */
object ManifestRules {

    /** Bumped only when the package layout changes in a way an older app cannot read. */
    const val FORMAT_VERSION = 1

    const val MANIFEST = "manifest.json"
    const val SETTINGS = "settings.json"
    const val DATABASE = "database/Absencium.db"
    const val ATTACHMENTS_DIR = "attachments/"

    /** Entry names the reader will accept. Anything else is refused, so a crafted zip cannot reach outside the staging area. */
    fun isSafeEntryName(name: String): Boolean {
        if (name == MANIFEST || name == SETTINGS || name == DATABASE) return true
        if (!name.startsWith(ATTACHMENTS_DIR)) return false
        val leaf = name.removePrefix(ATTACHMENTS_DIR)
        return leaf.isNotEmpty() && leaf.length <= 200 &&
            leaf != "." && leaf != ".." &&
            leaf.none { it == '/' || it == '\\' || it == '\u0000' }
    }

    fun sha256Hex(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    fun sha256Hex(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            digest.update(buffer, 0, n)
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    /**
     * Compares the manifest with what was actually read from the package ([actual] maps an entry name to its size and
     * SHA-256). Returns what is wrong, or an empty list when the package is intact and this app can read it.
     */
    fun problems(manifest: BackupManifest, actual: Map<String, Pair<Long, String>>, currentSchemaVersion: Int): List<String> {
        val out = mutableListOf<String>()
        if (manifest.formatVersion > FORMAT_VERSION || manifest.formatVersion < 1) {
            out += "This backup uses format ${manifest.formatVersion}; this app reads format $FORMAT_VERSION. Update Absencium and try again."
        }
        if (manifest.schemaVersion > currentSchemaVersion) {
            out += "This backup was made by a newer database version (${manifest.schemaVersion}; this app uses $currentSchemaVersion)."
        }
        if (manifest.schemaVersion < 1) out += "The backup does not say which database version it has."
        if (manifest.files.none { it.path == DATABASE }) out += "The backup has no database."
        val listed = manifest.files.associateBy { it.path }
        if (listed.size != manifest.files.size) out += "The manifest lists a file twice."
        for (file in manifest.files) {
            if (!isSafeEntryName(file.path)) {
                out += "The manifest names a file that is not allowed: ${file.path.take(60)}."
                continue
            }
            val found = actual[file.path]
            when {
                found == null -> out += "A file listed in the manifest is missing: ${file.path}."
                found.first != file.size -> out += "${file.path} has the wrong size."
                !found.second.equals(file.sha256, ignoreCase = true) ->
                    out += "${file.path} does not match its checksum; the backup is damaged or was changed."
            }
        }
        for (name in actual.keys) {
            if (name != MANIFEST && name !in listed) out += "The package holds a file the manifest does not list: ${name.take(60)}."
        }
        return out
    }
}
