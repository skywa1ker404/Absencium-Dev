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
    /** The parts the backup holds; null for a format 1 file, which always held everything. */
    val parts: Set<BackupPart>? = null,
    /** Only some parts can be restored: a format 2 file made by the same database version as this app. */
    val partRestorePossible: Boolean = false,
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

data class ExportResult(val databaseBytes: Long, val attachmentFiles: Int, val encrypted: Boolean, val includesAttachments: Boolean, val partCount: Int = 0)


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
    fun export(out: OutputStream, password: CharArray?, selection: Set<BackupPart>, settingsJson: String): ExportResult {
        val selected = BackupPart.withNeeds(selection)
        val excluded = BackupPart.entries.filter { it !in selected }
        val includeAttachments = selected.any { it.hasFiles }
        val database = AbsenciumDatabase.get(appContext).openHelper.writableDatabase
        val counts = tableCounts(database, excluded.flatMap { it.tables }.toSet())
        val schemaVersion = database.version
        val partInfo = estimateParts().filter { it.part in selected }
            .map { ManifestPart(it.part.name, it.rows, it.files, it.bytes) }
        val consistentCopy = copyDatabaseConsistently()
        try {
            // The exclusions are made in the copy, never in the live database; VACUUM then leaves no trace of an excluded part.
            if (excluded.isNotEmpty()) removePartsFromCopy(consistentCopy, excluded)
            val fileNames: Set<String>? =
                if (BackupPart.entries.filter { it.hasFiles }.all { it in selected }) null else referencedFileNames(consistentCopy, selected)
            val sealed: OutputStream = if (password != null) BackupCrypto.encrypt(out, password) else out
            val files = mutableListOf<ManifestFile>()
            var attachmentFiles = 0
            ZipOutputStream(BufferedOutputStream(sealed)).use { zip ->
                zip.setLevel(1)
                files += zip.addEntry(ManifestRules.DATABASE, consistentCopy.inputStream())
                if (BackupPart.SETTINGS in selected) {
                    files += zip.addEntry(ManifestRules.SETTINGS, settingsJson.toByteArray(Charsets.UTF_8).inputStream())
                }
                if (includeAttachments) {
                    attachmentDir.listFiles()?.filter { it.isFile && (fileNames == null || it.name in fileNames) }?.sortedBy { it.name }?.forEach { file ->
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
                    parts = partInfo,
                )
                zip.putNextEntry(ZipEntry(ManifestRules.MANIFEST))
                zip.write(BackupManifestJson.write(manifest))
                zip.closeEntry()
            }
            DebugLogger.i(TAG, "Backup written: db ${consistentCopy.length()} bytes, $attachmentFiles attachments, encrypted=${password != null}")
            return ExportResult(consistentCopy.length(), attachmentFiles, password != null, includeAttachments, selected.size)
        } finally {
            consistentCopy.delete()
            File(consistentCopy.path + "-wal").delete()
            File(consistentCopy.path + "-shm").delete()
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
                parts = manifest.parts.takeIf { it.isNotEmpty() }?.let { list ->
                    BackupPart.entries.filter { p -> list.any { it.id == p.name } }.toSet()
                },
                partRestorePossible = manifest.parts.isNotEmpty() && manifest.schemaVersion == currentVersion,
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

    /**
     * Records which parts of the staged backup to restore. Those parts are replaced and everything else on the phone is kept.
     * Choosing every part, or a file that cannot restore in part, means a whole restore as before. Settings are dropped when
     * that part is not chosen.
     */
    fun chooseStagedParts(parts: Set<BackupPart>?, merge: Boolean = false) {
        val file = File(appContext.filesDir, PARTS_FILE)
        val chosen = parts?.let { BackupPart.withNeeds(it) }
        if (chosen == null || (!merge && chosen.size == BackupPart.entries.size)) {
            file.delete()
            return
        }
        // A merge never touches the settings: there is no way to say which of two settings is the newer one.
        val kept = if (merge) chosen - BackupPart.SETTINGS else chosen
        file.writeText((if (merge) "MERGE" else "REPLACE") + "\n" + BackupPart.format(kept))
        if (BackupPart.SETTINGS !in kept) File(appContext.filesDir, SETTINGS_RESTORE).delete()
    }

    /** What the last start did with a restore in part, once, or null. */
    fun takeRestoreResult(): String? {
        val file = File(appContext.filesDir, RESULT_FILE)
        if (!file.exists()) return null
        val text = runCatching { file.readText() }.getOrNull()
        file.delete()
        return text
    }

    /** Drops a staged restore that the user decided not to apply. */
    fun cancelStagedRestore() {
        stagedRestoreFile().delete()
        File(appContext.filesDir, ATTACHMENTS_RESTORE).deleteRecursively()
        File(appContext.filesDir, SETTINGS_RESTORE).delete()
        File(appContext.filesDir, PARTS_FILE).delete()
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

    private fun tableCounts(db: androidx.sqlite.db.SupportSQLiteDatabase, excludedTables: Set<String> = emptySet()): Map<String, Int> {
        val tables = mapOf(
            "subjects" to "Subjects_Table", "attendance" to "Attendance_Table", "holidays" to "Holidays_Table",
            "exams" to "Exams_Table", "timetable" to "Timetable_Table", "notes" to "Notes_Table", "attachments" to "Attachments_Table",
        )
        return tables.mapValues { (_, table) ->
            if (table in excludedTables) return@mapValues 0
            runCatching { db.query("SELECT COUNT(*) FROM $table").use { if (it.moveToFirst()) it.getInt(0) else 0 } }.getOrDefault(0)
        }
    }

    /** Deletes the rows of the parts that are off from the backup copy (children first) and shrinks the file. */
    private fun removePartsFromCopy(copy: File, excluded: List<BackupPart>) {
        val db = SQLiteDatabase.openDatabase(copy.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            db.setForeignKeyConstraintsEnabled(false)
            db.beginTransaction()
            try {
                excluded.asReversed().forEach { part -> part.tables.forEach { db.execSQL("DELETE FROM $it") } }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            db.execSQL("VACUUM")
            db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
        } finally {
            db.close()
        }
    }

    /** The file names the rows of the chosen file parts point to, read from the filtered copy. */
    private fun referencedFileNames(copy: File, selected: Set<BackupPart>): Set<String> {
        val db = SQLiteDatabase.openDatabase(copy.path, null, SQLiteDatabase.OPEN_READONLY)
        val names = HashSet<String>()
        try {
            selected.filter { it.hasFiles }.forEach { part ->
                val column = if (part == BackupPart.DOCUMENTS) "local_uri" else "file_path"
                db.rawQuery("SELECT $column FROM ${part.tables.first()}", null).use { c ->
                    while (c.moveToNext()) c.getString(0)?.let { names += it.substringAfterLast('/').substringAfterLast('\\') }
                }
            }
        } finally {
            db.close()
        }
        return names
    }

    /**
     * What each part holds right now: rows, files and an approximate size, read with one small query per table (the database is
     * never loaded into memory). Sizes are "about": the text length of every column plus the files the rows point to.
     */
    fun estimateParts(): List<PartEstimate> {
        val db = AbsenciumDatabase.get(appContext).openHelper.readableDatabase
        return BackupPart.entries.map { part ->
            var rows = 0
            var bytes = 0L
            part.tables.forEach { table ->
                val (r, b) = tableRowsAndBytes(db, table)
                rows += r
                bytes += b
            }
            var files = 0
            if (part.hasFiles) {
                val column = if (part == BackupPart.DOCUMENTS) "local_uri" else "file_path"
                val (count, size) = filesOf(db, part.tables.first(), column)
                files = count
                bytes += size
            }
            PartEstimate(part, rows, files, bytes)
        }
    }

    private fun tableRowsAndBytes(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String): Pair<Int, Long> = runCatching {
        val columns = db.query("PRAGMA table_info($table)").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("name"))) }
        }
        val width = columns.joinToString("+") { "COALESCE(LENGTH(\"$it\"), 0)" }.ifEmpty { "0" }
        db.query("SELECT COUNT(*), COALESCE(SUM($width), 0) FROM $table").use { if (it.moveToFirst()) it.getInt(0) to it.getLong(1) else 0 to 0L }
    }.getOrDefault(0 to 0L)

    private fun filesOf(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String, column: String): Pair<Int, Long> = runCatching {
        var count = 0
        var size = 0L
        db.query("SELECT $column FROM $table").use { c ->
            while (c.moveToNext()) {
                val raw = c.getString(0) ?: continue
                val file = File(raw).takeIf { it.isFile } ?: File(attachmentDir, File(raw).name).takeIf { it.isFile } ?: continue
                count++
                size += file.length()
            }
        }
        count to size
    }.getOrDefault(0 to 0L)

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
        private const val PARTS_FILE = "restore_parts.txt"
        private const val RESULT_FILE = "restore_result.txt"
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
                    File(app.filesDir, PARTS_FILE).delete()
                    marker.delete()
                    return
                }
                if (!staged.exists()) return
                val partsFile = File(app.filesDir, PARTS_FILE)
                if (partsFile.exists()) {
                    marker.createNewFile()
                    if (dbFile.exists()) dbFile.copyTo(bak, overwrite = true)
                    val lines = runCatching { partsFile.readText() }.getOrNull().orEmpty().lines()
                    val merge = lines.firstOrNull() == "MERGE"
                    val chosen = BackupPart.parse(lines.getOrNull(1)).orEmpty()
                    val note = restoreParts(app, dbFile, staged, attachmentsStaged, attachments, chosen, merge)
                    File(app.filesDir, RESULT_FILE).writeText(note)
                    partsFile.delete()
                    staged.delete()
                    attachmentsStaged.deleteRecursively()
                    marker.delete()
                    return
                }
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

        /**
         * Replaces the chosen parts of the live database with those of the staged backup, in one transaction: the parts' rows
         * are deleted and the backup's rows inserted, then the foreign keys are checked, and any problem rolls everything back.
         * Both files have the same schema (checked at staging). Returns the sentence shown to the owner.
         */
        private fun restoreParts(app: Context, dbFile: File, staged: File, stagedFiles: File, filesDir: File, chosen: Set<BackupPart>, merge: Boolean): String {
            if (chosen.isEmpty()) return "Nothing was restored: no part was chosen."
            val src = SQLiteDatabase.openDatabase(staged.path, null, SQLiteDatabase.OPEN_READONLY)
            val dst = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
            try {
                dst.setForeignKeyConstraintsEnabled(false)
                var rows = 0
                var stats: MergeStats? = null
                dst.beginTransaction()
                try {
                    val parts = BackupPart.entries.filter { it in chosen }
                    if (merge) {
                        stats = BackupMerge.merge(src, dst, chosen, filesDir)
                    } else {
                        val present = parts.flatMap { it.tables }.filter { tableColumns(src, it).isNotEmpty() }.toSet()
                        parts.asReversed().forEach { p -> p.tables.filter { it in present }.forEach { dst.execSQL("DELETE FROM $it") } }
                        parts.forEach { p -> p.tables.filter { it in present }.forEach { rows += copyRows(src, dst, it) } }
                    }
                    if (!merge && BackupPart.NOTE_FILES in chosen) {
                        dst.rawQuery("SELECT id, file_path FROM Attachments_Table", null).use { c ->
                            val fix = mutableListOf<Pair<Long, String>>()
                            while (c.moveToNext()) fix += c.getLong(0) to c.getString(1)
                            for ((id, path) in fix) {
                                val leaf = path.substringAfterLast('/').substringAfterLast('\\')
                                dst.execSQL("UPDATE Attachments_Table SET file_path = ? WHERE id = ?", arrayOf<Any>(File(filesDir, leaf).absolutePath, id))
                            }
                        }
                    }
                    val broken = dst.rawQuery("PRAGMA foreign_key_check", null).use { if (it.moveToFirst()) it.getString(0) else null }
                    if (broken != null) {
                        throw IllegalStateException("rows in $broken point to data that is not on this phone; switch on the parts they need")
                    }
                    dst.setTransactionSuccessful()
                } finally {
                    dst.endTransaction()
                }
                var files = 0
                if (stagedFiles.exists()) {
                    filesDir.mkdirs()
                    chosen.filter { it.hasFiles }.forEach { part ->
                        val column = if (part == BackupPart.DOCUMENTS) "local_uri" else "file_path"
                        dst.rawQuery("SELECT $column FROM ${part.tables.first()}", null).use { c ->
                            while (c.moveToNext()) {
                                val leaf = c.getString(0)?.substringAfterLast('/')?.substringAfterLast('\\') ?: continue
                                val from = File(stagedFiles, leaf)
                                val to = File(filesDir, leaf)
                                if (from.isFile && (!merge || !to.exists())) {
                                    from.copyTo(to, overwrite = true)
                                    files++
                                }
                            }
                        }
                    }
                }
                val merged = stats
                if (merged != null) {
                    DebugLogger.i(TAG, "Merged ${chosen.size} parts: $merged, $files files")
                    return "Merged ${chosen.size} parts from the backup: ${merged.added} added, ${merged.updated} updated to the newer version, " +
                        "${merged.kept} already there or newer on this phone" +
                        (if (merged.skipped > 0) ", ${merged.skipped} skipped because what they belong to is not on this phone" else "") +
                        ", $files files. Nothing was deleted."
                }
                DebugLogger.i(TAG, "Restored ${chosen.size} parts ($rows rows, $files files) from the backup")
                return "Restored ${chosen.size} parts from the backup ($rows items, $files files). Everything else was kept."
            } catch (e: Exception) {
                DebugLogger.e(TAG, "Restoring parts failed; nothing was changed", e)
                return "The restore was not applied and nothing was changed: ${e.message ?: e.javaClass.simpleName}."
            } finally {
                src.close()
                dst.close()
            }
        }

        private fun tableColumns(db: SQLiteDatabase, table: String): List<String> =
            db.rawQuery("PRAGMA table_info($table)", null).use { c ->
                buildList { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("name"))) }
            }

        private fun copyRows(src: SQLiteDatabase, dst: SQLiteDatabase, table: String): Int {
            val sourceColumns = tableColumns(src, table).toSet()
            val columns = tableColumns(dst, table).filter { it in sourceColumns }
            if (columns.isEmpty()) return 0
            val quoted = columns.joinToString(",") { "\"$it\"" }
            val statement = dst.compileStatement("INSERT INTO $table ($quoted) VALUES (${columns.joinToString(",") { "?" }})")
            var count = 0
            try {
                src.rawQuery("SELECT $quoted FROM $table", null).use { c ->
                    while (c.moveToNext()) {
                        statement.clearBindings()
                        for (i in columns.indices) {
                            when (c.getType(i)) {
                                android.database.Cursor.FIELD_TYPE_NULL -> statement.bindNull(i + 1)
                                android.database.Cursor.FIELD_TYPE_INTEGER -> statement.bindLong(i + 1, c.getLong(i))
                                android.database.Cursor.FIELD_TYPE_FLOAT -> statement.bindDouble(i + 1, c.getDouble(i))
                                android.database.Cursor.FIELD_TYPE_BLOB -> statement.bindBlob(i + 1, c.getBlob(i))
                                else -> statement.bindString(i + 1, c.getString(i))
                            }
                        }
                        statement.executeInsert()
                        count++
                    }
                }
            } finally {
                statement.close()
            }
            return count
        }

        private fun deleteDatabaseFiles(dbFile: File) {
            listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
                File(dbFile.parentFile, dbFile.name + suffix).delete()
            }
        }
    }
}
