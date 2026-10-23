package skywa1ker404.absencium.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import skywa1ker404.absencium.data.db.AbsenciumDatabase
import skywa1ker404.absencium.data.storage.AttachmentStore
import skywa1ker404.absencium.logging.DebugLogger
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** What a backup holds, shown to the user before a restore is staged. */
data class BackupSummary(
    val createdAt: Long,
    val appVersion: String,
    val schemaVersion: Int,
    val encrypted: Boolean,
    val includesAttachments: Boolean,
    val counts: Map<String, Int>,
    val attachmentFiles: Int,
    /** The database in the backup is byte for byte the one on this phone right now. */
    val sameAsCurrent: Boolean,
)

sealed interface StageResult {
    data class Ready(val summary: BackupSummary, val settingsJson: String?) : StageResult
    data object NeedsPassword : StageResult
    data class Failed(val message: String, val passwordProblem: Boolean = false) : StageResult
}

data class ExportResult(val databaseBytes: Long, val attachmentFiles: Int, val encrypted: Boolean, val includesAttachments: Boolean)


/**
 * Backup and restore as one versioned package: a zip with `database/Absencium.db`, the vault's attachment files,
 * `settings.json` (no secrets; the AI key never leaves its vault) and a `manifest.json` last, listing every other entry with
 * its size and SHA-256. The zip can be wrapped in [BackupCrypto] for a password.
 *
 * A restore cannot replace the database while the app is using it, so [stage] validates the package completely and parks
 * the result next to the live files. [applyStagedRestore] swaps it in on the next app start, before the database opens.
 * The previous database and attachments are kept (`.bak`), and a marker file lets a swap that was cut off be rolled back.
 */
class BackupManager(context: Context) {

    private val appContext = context.applicationContext
    private val dbFile: File get() = appContext.getDatabasePath(AbsenciumDatabase.DATABASE_NAME)
    private val attachmentDir: File get() = File(appContext.filesDir, AttachmentStore.FOLDER)

    /**
     * Writes the package to [out]. [password] seals it with AES-256-GCM; null leaves it as a plain zip.
     * [includeAttachments] adds the photos, documents and recordings of the vault.
     */
    fun export(out: OutputStream, password: CharArray?, includeAttachments: Boolean, settingsJson: String): ExportResult {
        val database = AbsenciumDatabase.get(appContext).openHelper.writableDatabase
        val counts = tableCounts(database)
        val schemaVersion = database.version
        val consistentCopy = copyDatabaseConsistently()
        try {
            val sealed: OutputStream = if (password != null) BackupCrypto.encrypt(out, password) else out
            val files = mutableListOf<ManifestFile>()
            var attachmentFiles = 0
            ZipOutputStream(BufferedOutputStream(sealed)).use { zip ->
                zip.setLevel(1)
                files += zip.addEntry(ManifestRules.DATABASE, consistentCopy.inputStream())
                files += zip.addEntry(ManifestRules.SETTINGS, settingsJson.toByteArray(Charsets.UTF_8).inputStream())
                if (includeAttachments) {
                    attachmentDir.listFiles()?.filter { it.isFile }?.sortedBy { it.name }?.forEach { file ->
                        val name = ManifestRules.ATTACHMENTS_DIR + file.name
                        if (ManifestRules.isSafeEntryName(name)) {
                            files += zip.addEntry(name, file.inputStream())
                            attachmentFiles++
                        }
                    }
                }
                val manifest = BackupManifest(
                    formatVersion = ManifestRules.FORMAT_VERSION,
                    appVersion = appVersion(),
                    schemaVersion = schemaVersion,
                    createdAt = System.currentTimeMillis(),
                    includesAttachments = includeAttachments,
                    encrypted = password != null,
                    counts = counts,
                    files = files,
                )
                zip.putNextEntry(ZipEntry(ManifestRules.MANIFEST))
                zip.write(BackupManifestJson.write(manifest))
                zip.closeEntry()
            }
            DebugLogger.i(TAG, "Backup written: db ${consistentCopy.length()} bytes, $attachmentFiles attachments, encrypted=${password != null}")
            return ExportResult(consistentCopy.length(), attachmentFiles, password != null, includeAttachments)
        } finally {
            consistentCopy.delete()
        }
    }

    /**
     * Validates the package in [input] and stages it for the next app start. Nothing live is touched until then.
     * [expectedBytes], when known, is used for a low-storage check before anything is written.
     */
    fun stage(input: InputStream, password: CharArray?, expectedBytes: Long? = null): StageResult {
        if (expectedBytes != null && appContext.filesDir.usableSpace < expectedBytes * 2 + MIN_FREE_BYTES) {
            return StageResult.Failed("There is not enough free storage to check and stage this backup. Free some space and try again.")
        }
        val staging = File(appContext.filesDir, STAGING_DIR)
        staging.deleteRecursively()
        staging.mkdirs()
        try {
            val buffered = BufferedInputStream(input, 64 * 1024)
            buffered.mark(BackupCrypto.MAGIC_LENGTH + 1)
            val head = ByteArray(BackupCrypto.MAGIC_LENGTH)
            var got = 0
            while (got < head.size) {
                val n = buffered.read(head, got, head.size - got)
                if (n < 0) break
                got += n
            }
            buffered.reset()
            val encrypted = BackupCrypto.looksEncrypted(head.copyOf(got))
            val source: InputStream = if (encrypted) {
                if (password == null) return StageResult.NeedsPassword
                try {
                    BackupCrypto.decrypt(buffered, password)
                } catch (e: BackupDecryptionException) {
                    return StageResult.Failed(e.message ?: "Could not open the backup.", passwordProblem = true)
                }
            } else {
                buffered
            }
            return readPackage(source, staging, encrypted)
        } catch (e: BackupDecryptionException) {
            return StageResult.Failed(e.message ?: "Could not open the backup.", passwordProblem = true)
        } catch (e: IOException) {
            DebugLogger.e(TAG, "Could not read the backup", e)
            val full = e.message?.contains("ENOSPC", ignoreCase = true) == true || e.message?.contains("No space", ignoreCase = true) == true
            return StageResult.Failed(
                if (full) "There is not enough free storage to restore this backup." else "Could not read this backup: ${e.message ?: e.javaClass.simpleName}",
            )
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Could not read the backup", e)
            return StageResult.Failed("This file is not a usable Absencium backup (${e.message ?: e.javaClass.simpleName}).")
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun readPackage(source: InputStream, staging: File, encrypted: Boolean): StageResult {
        val stagedDb = File(staging, "db.tmp")
        val stagedAttachments = File(staging, "attachments").also { it.mkdirs() }
        val actual = LinkedHashMap<String, Pair<Long, String>>()
        var manifestBytes: ByteArray? = null
        var settingsBytes: ByteArray? = null
        var legacy = false
        ZipInputStream(source).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val name = entry.name
                if (name != LEGACY_DATABASE && !ManifestRules.isSafeEntryName(name)) {
                    return StageResult.Failed("This package contains an entry that is not allowed (${name.take(40)}), so it was not opened.")
                }
                if (name in actual || (name == ManifestRules.MANIFEST && manifestBytes != null)) {
                    return StageResult.Failed("This package lists the same file twice, so it was not opened.")
                }
                when (name) {
                    ManifestRules.MANIFEST -> manifestBytes = zip.readLimited(MAX_SMALL_ENTRY)
                    ManifestRules.SETTINGS -> {
                        val bytes = zip.readLimited(MAX_SMALL_ENTRY)
                        settingsBytes = bytes
                        actual[name] = bytes.size.toLong() to ManifestRules.sha256Hex(bytes)
                    }
                    ManifestRules.DATABASE -> actual[name] = zip.copyHashed(stagedDb)
                    // Backups made before the versioned format held only the database, under this name.
                    LEGACY_DATABASE -> {
                        actual[ManifestRules.DATABASE] = zip.copyHashed(stagedDb)
                        legacy = true
                    }
                    else -> actual[name] = zip.copyHashed(File(stagedAttachments, name.removePrefix(ManifestRules.ATTACHMENTS_DIR)))
                }
            }
        }
        val rawManifest = manifestBytes
        val manifest = if (rawManifest == null && legacy && actual[ManifestRules.DATABASE] != null) {
            // No manifest to check against: the database itself is still validated below.
            val (size, hash) = actual.getValue(ManifestRules.DATABASE)
            val version = try {
                SQLiteDatabase.openDatabase(stagedDb.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
            } catch (e: Exception) {
                return StageResult.Failed("The database inside this backup could not be opened.")
            }
            BackupManifest(1, "", version, 0L, false, encrypted, emptyMap(), listOf(ManifestFile(ManifestRules.DATABASE, size, hash)))
        } else {
            if (rawManifest == null) return StageResult.Failed("This zip has no manifest. It is not an Absencium backup.")
            try {
                BackupManifestJson.read(rawManifest)
            } catch (e: IllegalArgumentException) {
                return StageResult.Failed(e.message ?: "The manifest is damaged.")
            }
        }
        val currentVersion = AbsenciumDatabase.get(appContext).openHelper.readableDatabase.version
        val problems = ManifestRules.problems(manifest, actual, currentVersion)
        if (problems.isNotEmpty()) return StageResult.Failed(problems.take(3).joinToString(" "))

        val header = ByteArray(SQLITE_HEADER.size)
        val read = stagedDb.inputStream().use { it.read(header) }
        if (read != header.size || !header.contentEquals(SQLITE_HEADER)) {
            return StageResult.Failed("The database inside this backup is not a valid SQLite file.")
        }
        SQLiteDatabase.openDatabase(stagedDb.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            val check = db.rawQuery("PRAGMA quick_check", null).use { if (it.moveToFirst()) it.getString(0) else "no result" }
            if (check != "ok") return StageResult.Failed("The database inside this backup is damaged ($check).")
            if (db.version != manifest.schemaVersion) {
                return StageResult.Failed("The database version inside this backup does not match its manifest.")
            }
            pointAttachmentsHere(db)
        }

        val liveDb = dbFile
        val sameAsCurrent = runCatching {
            checkpoint()
            liveDb.exists() && ManifestRules.sha256Hex(liveDb.inputStream()) == actual[ManifestRules.DATABASE]?.second
        }.getOrDefault(false)

        val parked = stagedRestoreFile()
        parked.delete()
        if (!stagedDb.renameTo(parked)) {
            stagedDb.copyTo(parked, overwrite = true)
        }
        val parkedAttachments = File(appContext.filesDir, ATTACHMENTS_RESTORE)
        parkedAttachments.deleteRecursively()
        if (manifest.includesAttachments) {
            if (!stagedAttachments.renameTo(parkedAttachments)) {
                stagedAttachments.copyRecursively(parkedAttachments, overwrite = true)
            }
        }
        File(appContext.filesDir, SETTINGS_RESTORE).let { f ->
            f.delete()
            settingsBytes?.let { f.writeBytes(it) }
        }
        DebugLogger.i(TAG, "Restore staged (schema ${manifest.schemaVersion}, attachments=${manifest.includesAttachments}), applies on next app start")
        return StageResult.Ready(
            summary = BackupSummary(
                createdAt = manifest.createdAt,
                appVersion = manifest.appVersion,
                schemaVersion = manifest.schemaVersion,
                encrypted = encrypted,
                includesAttachments = manifest.includesAttachments,
                counts = manifest.counts,
                attachmentFiles = manifest.files.count { it.path.startsWith(ManifestRules.ATTACHMENTS_DIR) },
                sameAsCurrent = sameAsCurrent,
            ),
            settingsJson = settingsBytes?.toString(Charsets.UTF_8),
        )
    }

    /** Attachment rows store an absolute path, which differs between phones and users; point them at this app's folder. */
    private fun pointAttachmentsHere(db: SQLiteDatabase) {
        try {
            val rows = mutableListOf<Pair<Long, String>>()
            db.rawQuery("SELECT id, file_path FROM Attachments_Table", null).use {
                while (it.moveToNext()) rows += it.getLong(0) to it.getString(1)
            }
            db.beginTransaction()
            try {
                for ((id, path) in rows) {
                    val leaf = path.substringAfterLast('/').substringAfterLast('\\')
                    db.execSQL("UPDATE Attachments_Table SET file_path = ? WHERE id = ?", arrayOf<Any>(File(attachmentDir, leaf).absolutePath, id))
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            DebugLogger.w(TAG, "Could not rewrite attachment paths: ${e.message}")
        }
    }

    /** Settings that came with a staged restore, once, or null. Read and removed at the next start, after the swap. */
    fun takeStagedSettings(): String? {
        val file = File(appContext.filesDir, SETTINGS_RESTORE)
        if (!file.exists()) return null
        val text = runCatching { file.readText() }.getOrNull()
        file.delete()
        return text
    }

    fun hasStagedRestore(): Boolean = stagedRestoreFile().exists()

    /** Drops a staged restore that the user decided not to apply. */
    fun cancelStagedRestore() {
        stagedRestoreFile().delete()
        File(appContext.filesDir, ATTACHMENTS_RESTORE).deleteRecursively()
        File(appContext.filesDir, SETTINGS_RESTORE).delete()
    }

    // ------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------

    private fun checkpoint() {
        AbsenciumDatabase.get(appContext).openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
    }

    /**
     * Copies the database file after moving the write-ahead log into it. A write that slips in between would leave the copy
     * without that change (not corrupt, but not what the manifest promises), so the copy is repeated until the log is still
     * empty and the file unchanged when it finishes.
     */
    private fun copyDatabaseConsistently(): File {
        val copy = File(appContext.cacheDir, "backup-db.tmp")
        val wal = File(dbFile.parentFile, dbFile.name + "-wal")
        repeat(4) {
            checkpoint()
            val sizeBefore = dbFile.length()
            val timeBefore = dbFile.lastModified()
            dbFile.inputStream().use { i -> copy.outputStream().use { o -> i.copyTo(o) } }
            if ((!wal.exists() || wal.length() == 0L) && dbFile.length() == sizeBefore && dbFile.lastModified() == timeBefore) return copy
        }
        // Writes keep arriving; the last copy is still a valid SQLite file, only possibly a moment behind.
        DebugLogger.w(TAG, "The database kept changing during the backup copy; using the last copy")
        return copy
    }

    private fun tableCounts(db: androidx.sqlite.db.SupportSQLiteDatabase): Map<String, Int> {
        val tables = mapOf(
            "subjects" to "Subjects_Table", "attendance" to "Attendance_Table", "holidays" to "Holidays_Table",
            "exams" to "Exams_Table", "timetable" to "Timetable_Table", "notes" to "Notes_Table", "attachments" to "Attachments_Table",
        )
        return tables.mapValues { (_, table) ->
            runCatching { db.query("SELECT COUNT(*) FROM $table").use { if (it.moveToFirst()) it.getInt(0) else 0 } }.getOrDefault(0)
        }
    }

    @Suppress("DEPRECATION")
    private fun appVersion(): String = try {
        appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName.orEmpty()
    } catch (e: Exception) {
        ""
    }

    private fun stagedRestoreFile() = stagedFile(dbFile)

    private fun ZipOutputStream.addEntry(name: String, source: InputStream): ManifestFile {
        putNextEntry(ZipEntry(name))
        val digest = MessageDigest.getInstance("SHA-256")
        var size = 0L
        val buffer = ByteArray(64 * 1024)
        source.use { input ->
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
                write(buffer, 0, n)
                size += n
            }
        }
        closeEntry()
        return ManifestFile(name, size, digest.digest().joinToString("") { "%02x".format(it) })
    }

    private fun ZipInputStream.copyHashed(target: File): Pair<Long, String> {
        val digest = MessageDigest.getInstance("SHA-256")
        var size = 0L
        val buffer = ByteArray(64 * 1024)
        target.outputStream().use { out ->
            while (true) {
                val n = read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
                out.write(buffer, 0, n)
                size += n
            }
        }
        return size to digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun ZipInputStream.readLimited(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val n = read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
            if (out.size() > limit) throw IOException("An entry in this package is unusually large.")
        }
        return out.toByteArray()
    }

    companion object {
        private const val TAG = "Backup"
        private const val LEGACY_DATABASE = "Absencium.db"
        private const val STAGING_DIR = "restore_staging"
        private const val ATTACHMENTS_RESTORE = "vault_attachments.restore"
        private const val SETTINGS_RESTORE = "settings.restore"
        private const val ATTACHMENTS_BAK = "vault_attachments.bak"
        private const val MAX_SMALL_ENTRY = 1024 * 1024
        private const val MIN_FREE_BYTES = 32L * 1024 * 1024
        private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

        private fun stagedFile(dbFile: File) = File(dbFile.parentFile, "${dbFile.name}.restore")

        /**
         * Call once at app start, before anything opens the database. Finishes a staged restore, or, if a previous swap was
         * cut off (the marker file is still there), puts the previous data back first.
         */
        fun applyStagedRestore(context: Context) {
            val app = context.applicationContext
            val dbFile = app.getDatabasePath(AbsenciumDatabase.DATABASE_NAME)
            val dbDir = dbFile.parentFile ?: return
            val staged = stagedFile(dbFile)
            val bak = File(dbDir, "${dbFile.name}.bak")
            val marker = File(dbDir, "${dbFile.name}.restoring")
            val attachments = File(app.filesDir, AttachmentStore.FOLDER)
            val attachmentsBak = File(app.filesDir, ATTACHMENTS_BAK)
            val attachmentsStaged = File(app.filesDir, ATTACHMENTS_RESTORE)
            try {
                if (marker.exists()) {
                    DebugLogger.w(TAG, "A restore was interrupted; rolling back to the previous data")
                    if (bak.exists()) {
                        deleteDatabaseFiles(dbFile)
                        bak.copyTo(dbFile, overwrite = true)
                    }
                    if (attachmentsBak.exists()) {
                        attachments.deleteRecursively()
                        attachmentsBak.renameTo(attachments)
                    }
                    staged.delete()
                    attachmentsStaged.deleteRecursively()
                    File(app.filesDir, SETTINGS_RESTORE).delete()
                    marker.delete()
                    return
                }
                if (!staged.exists()) return
                marker.createNewFile()
                if (dbFile.exists()) dbFile.copyTo(bak, overwrite = true)
                deleteDatabaseFiles(dbFile)
                if (!staged.renameTo(dbFile)) {
                    staged.copyTo(dbFile, overwrite = true)
                    staged.delete()
                }
                if (attachmentsStaged.exists()) {
                    attachmentsBak.deleteRecursively()
                    if (attachments.exists()) attachments.renameTo(attachmentsBak)
                    if (!attachmentsStaged.renameTo(attachments)) {
                        attachmentsStaged.copyRecursively(attachments, overwrite = true)
                        attachmentsStaged.deleteRecursively()
                    }
                }
                marker.delete()
                DebugLogger.i(TAG, "Restored database from backup (previous one kept as ${bak.name})")
            } catch (e: Exception) {
                DebugLogger.e(TAG, "Applying the staged restore failed", e)
            }
        }

        private fun deleteDatabaseFiles(dbFile: File) {
            listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
                File(dbFile.parentFile, dbFile.name + suffix).delete()
            }
        }
    }
}
