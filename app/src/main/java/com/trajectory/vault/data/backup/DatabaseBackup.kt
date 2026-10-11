package com.trajectory.vault.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.trajectory.vault.data.db.TrajectoryDatabase
import com.trajectory.vault.logging.DebugLogger
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup and restore of `Trajectory.db` as a .zip file.
 *
 * A restore cannot replace the database while the app is using it, so [stageRestore] validates
 * the backup and parks it next to the database. [applyStagedRestore] swaps it in on the next
 * app start, before the database is opened. The previous database is kept as `Trajectory.db.bak`.
 */
class DatabaseBackup(context: Context) {

    private val appContext = context.applicationContext

    /** Writes a zip containing a consistent copy of the database to [out]. Returns the database size in bytes. */
    fun export(out: OutputStream): Long {
        val database = TrajectoryDatabase.get(appContext).openHelper.writableDatabase
        // Move everything still in the write-ahead log into the main file so the copy is complete.
        database.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val file = appContext.getDatabasePath(TrajectoryDatabase.DATABASE_NAME)
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(ENTRY_NAME))
            file.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
        DebugLogger.i(TAG, "Backup written: ${file.length()} bytes of database")
        return file.length()
    }

    /**
     * Validates the backup in [input] and stages it for the next app start.
     * Returns null on success, or a message explaining why the file cannot be used.
     */
    fun stageRestore(input: InputStream): String? {
        val dbFile = appContext.getDatabasePath(TrajectoryDatabase.DATABASE_NAME)
        val staged = stagedFile(dbFile)
        try {
            var found = false
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                // The entry name is only compared, never used as a path, so a crafted zip cannot write elsewhere.
                while (entry != null) {
                    if (!entry.isDirectory && entry.name == ENTRY_NAME) {
                        staged.outputStream().use { zip.copyTo(it) }
                        found = true
                        break
                    }
                    entry = zip.nextEntry
                }
            }
            if (!found) {
                return "This zip does not contain ${TrajectoryDatabase.DATABASE_NAME}."
            }

            val header = ByteArray(SQLITE_HEADER.size)
            val read = staged.inputStream().use { it.read(header) }
            if (read != header.size || !header.contentEquals(SQLITE_HEADER)) {
                staged.delete()
                return "The database inside this zip is not a valid SQLite file."
            }

            val backupVersion = SQLiteDatabase.openDatabase(staged.path, null, SQLiteDatabase.OPEN_READONLY)
                .use { it.version }
            val currentVersion = TrajectoryDatabase.get(appContext).openHelper.readableDatabase.version
            // An older backup is migrated when the app opens it; a newer one cannot be read.
            if (backupVersion > currentVersion || backupVersion < 1) {
                staged.delete()
                return "This backup was made by a newer database version ($backupVersion, this app uses $currentVersion)."
            }
            DebugLogger.i(TAG, "Restore staged, applies on next app start")
            return null
        } catch (e: Exception) {
            staged.delete()
            DebugLogger.e(TAG, "Could not read the backup file", e)
            return "Could not read this backup: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    fun hasStagedRestore(): Boolean =
        stagedFile(appContext.getDatabasePath(TrajectoryDatabase.DATABASE_NAME)).exists()

    companion object {
        private const val TAG = "Backup"
        private const val ENTRY_NAME = TrajectoryDatabase.DATABASE_NAME
        private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

        private fun stagedFile(dbFile: File) = File(dbFile.parentFile, "${dbFile.name}.restore")

        /** Call once at app start, before anything opens the database. */
        fun applyStagedRestore(context: Context) {
            val dbFile = context.getDatabasePath(TrajectoryDatabase.DATABASE_NAME)
            val staged = stagedFile(dbFile)
            if (!staged.exists()) return
            try {
                if (dbFile.exists()) {
                    dbFile.copyTo(File(dbFile.parentFile, "${dbFile.name}.bak"), overwrite = true)
                }
                listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
                    File(dbFile.parentFile, dbFile.name + suffix).delete()
                }
                if (!staged.renameTo(dbFile)) {
                    staged.copyTo(dbFile, overwrite = true)
                    staged.delete()
                }
                DebugLogger.i(TAG, "Restored database from backup (previous one kept as ${dbFile.name}.bak)")
            } catch (e: Exception) {
                DebugLogger.e(TAG, "Applying the staged restore failed", e)
            }
        }
    }
}
