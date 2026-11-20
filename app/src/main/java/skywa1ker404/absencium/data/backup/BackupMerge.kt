package skywa1ker404.absencium.data.backup

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDoneException
import android.database.sqlite.SQLiteException
import java.io.File
import java.util.UUID

/** What a merge did, in rows. */
data class MergeStats(var added: Int = 0, var updated: Int = 0, var kept: Int = 0, var skipped: Int = 0)

/**
 * Merges the chosen parts of a backup into the live database (docs/BACKUP_DESIGN.md, section 7) without deleting anything:
 *
 * - Rows with a `uuid` (subjects, attendance, holidays, exams, timetable, notes, attachments, documents, syllabus) are matched by it.
 *   A subject with the same name counts as the same subject. A row only on the backup is added; a row on both is replaced by
 *   the backup's only when its `updated_at` is strictly newer, so ties keep the phone's.
 * - Rows without a `uuid` are added unless an identical row is already there.
 * - Links between rows (`subject_id`, `note_id`, `parent_id`, ...) are rewritten to the ids this phone gave the matching rows. A row whose
 *   parent is not on the phone is skipped and counted, never attached to the wrong row.
 *
 * Both databases have the same schema (checked when the backup was staged). The caller runs this in one transaction.
 */
internal object BackupMerge {

    /** Link columns and the table each points at ("" means the table itself). A link column not listed here stops the merge. */
    private val LINKS = mapOf(
        "subject_id" to "Subjects_Table",
        "note_id" to "Notes_Table",
        "parent_id" to "",
        "syllabus_node_id" to "Syllabus_Nodes_Table",
        "question_id" to "Recall_Quiz_Table",
    )

    /** Columns that hold a path to a file inside the attachments folder. */
    private val PATHS = setOf("file_path", "local_uri")

    fun merge(src: SQLiteDatabase, dst: SQLiteDatabase, chosen: Set<BackupPart>, filesDir: File): MergeStats {
        val stats = MergeStats()
        val maps = HashMap<String, HashMap<Long, Long>>()
        val parts = BackupPart.entries.filter { it in chosen && it.tables.isNotEmpty() }
        // Links to a part that is not being merged still have to land on the matching rows already on the phone.
        if (BackupPart.SUBJECTS !in chosen) matchOnly(src, dst, "Subjects_Table", maps)
        if (BackupPart.NOTES !in chosen) matchOnly(src, dst, "Notes_Table", maps)
        if (BackupPart.SYLLABUS !in chosen) matchOnly(src, dst, "Syllabus_Nodes_Table", maps)
        for (part in parts) for (table in part.tables) mergeTable(src, dst, table, maps, filesDir, stats)
        return stats
    }

    fun columns(db: SQLiteDatabase, table: String): List<ColumnInfo> =
        db.rawQuery("PRAGMA table_info($table)", null).use { c ->
            buildList {
                while (c.moveToNext()) add(ColumnInfo(c.getString(c.getColumnIndexOrThrow("name")), c.getInt(c.getColumnIndexOrThrow("notnull")) == 1))
            }
        }

    data class ColumnInfo(val name: String, val notNull: Boolean)

    private fun matchOnly(src: SQLiteDatabase, dst: SQLiteDatabase, table: String, maps: HashMap<String, HashMap<Long, Long>>) {
        val map = maps.getOrPut(table) { HashMap() }
        val hasName = table == "Subjects_Table"
        src.rawQuery("SELECT id, uuid${if (hasName) ", name" else ""} FROM $table", null).use { c ->
            while (c.moveToNext()) {
                val found = findByUuid(dst, table, c.getString(1)) ?: if (hasName) findByName(dst, c.getString(2)) else null
                if (found != null) map[c.getLong(0)] = found.first
            }
        }
    }

    private fun findByUuid(db: SQLiteDatabase, table: String, uuid: String?): Pair<Long, Long>? {
        if (uuid.isNullOrBlank()) return null
        return db.rawQuery("SELECT id, updated_at FROM $table WHERE uuid = ?", arrayOf(uuid)).use {
            if (it.moveToFirst()) it.getLong(0) to it.getLong(1) else null
        }
    }

    private fun findByName(db: SQLiteDatabase, name: String?): Pair<Long, Long>? {
        if (name == null) return null
        return db.rawQuery("SELECT id, updated_at FROM Subjects_Table WHERE name = ?", arrayOf(name)).use {
            if (it.moveToFirst()) it.getLong(0) to it.getLong(1) else null
        }
    }

    private fun mergeTable(
        src: SQLiteDatabase, dst: SQLiteDatabase, table: String,
        maps: HashMap<String, HashMap<Long, Long>>, filesDir: File, stats: MergeStats,
    ) {
        val srcNames = columns(src, table).map { it.name }.toSet()
        if (srcNames.isEmpty()) return
        val dstColumns = columns(dst, table)
        val cols = dstColumns.filter { it.name in srcNames && it.name != "id" }
        if (cols.isEmpty()) return
        val names = cols.map { it.name }
        val notNull = cols.filter { it.notNull }.map { it.name }.toSet()
        for (name in names) {
            if (name.endsWith("_id") && name !in LINKS) throw IllegalStateException("the column $name of $table is a link this version cannot merge")
        }
        val hasUuid = "uuid" in names
        val hasUpdated = "updated_at" in names
        val map = maps.getOrPut(table) { HashMap() }
        val quoted = names.joinToString(",") { "\"$it\"" }
        val insert = dst.compileStatement("INSERT INTO $table ($quoted) VALUES (${names.joinToString(",") { "?" }})")
        val updateNames = names.filter { it != "uuid" }
        val update = if (hasUuid && updateNames.isNotEmpty()) {
            dst.compileStatement("UPDATE $table SET ${updateNames.joinToString(",") { "\"$it\" = ?" }} WHERE id = ?")
        } else null
        val selfLinked = "parent_id" in names
        try {
            var pass = 0
            while (true) {
                var progressed = false
                var deferred = 0
                src.rawQuery("SELECT id, $quoted FROM $table ORDER BY id", null).use { c ->
                    while (c.moveToNext()) {
                        val srcId = c.getLong(0)
                        if (srcId in map) continue
                        val values = ArrayList<Any?>(names.size)
                        for (i in names.indices) values += read(c, i + 1)
                        var unresolved = false
                        var dropped = false
                        for ((i, name) in names.withIndex()) {
                            val v = values[i]
                            if (name in PATHS && v is String && !v.contains("://") && v.isNotEmpty()) {
                                values[i] = File(filesDir, v.substringAfterLast('/').substringAfterLast('\\')).absolutePath
                            }
                            val target = LINKS[name] ?: continue
                            if (v !is Long) continue
                            val linked = maps[target.ifEmpty { table }]?.get(v)?.takeIf { it >= 0 }
                            if (linked != null) {
                                values[i] = linked
                            } else if (target.isEmpty() && pass < 20 && selfLinked) {
                                unresolved = true
                            } else if (name in notNull) {
                                dropped = true
                            } else {
                                values[i] = null
                            }
                        }
                        if (unresolved) { deferred++; continue }
                        if (dropped) { stats.skipped++; map[srcId] = -1L; continue }
                        progressed = true
                        mergeRow(dst, table, names, values, hasUuid, hasUpdated, insert, update, updateNames, map, srcId, stats)
                    }
                }
                pass++
                if (!selfLinked || deferred == 0 || !progressed || pass >= 20) {
                    stats.skipped += deferred
                    break
                }
            }
        } finally {
            insert.close()
            update?.close()
        }
        map.values.removeAll { it == -1L }
    }

    private fun mergeRow(
        dst: SQLiteDatabase, table: String, names: List<String>, values: ArrayList<Any?>, hasUuid: Boolean, hasUpdated: Boolean,
        insert: android.database.sqlite.SQLiteStatement, update: android.database.sqlite.SQLiteStatement?, updateNames: List<String>,
        map: HashMap<Long, Long>, srcId: Long, stats: MergeStats,
    ) {
        try {
            if (hasUuid) {
                val uuidAt = names.indexOf("uuid")
                val uuid = values[uuidAt] as? String
                val found = findByUuid(dst, table, uuid)
                    ?: if (table == "Subjects_Table") findByName(dst, values[names.indexOf("name")] as? String) else null
                if (found != null) {
                    map[srcId] = found.first
                    val backupUpdated = if (hasUpdated) (values[names.indexOf("updated_at")] as? Long ?: 0L) else 0L
                    if (update != null && hasUpdated && backupUpdated > found.second) {
                        update.clearBindings()
                        updateNames.forEachIndexed { i, n -> bind(update, i + 1, values[names.indexOf(n)]) }
                        update.bindLong(updateNames.size + 1, found.first)
                        update.executeUpdateDelete()
                        stats.updated++
                    } else {
                        stats.kept++
                    }
                    return
                }
                if (uuid.isNullOrBlank()) values[uuidAt] = UUID.randomUUID().toString()
            } else {
                val same = findIdentical(dst, table, names, values)
                if (same != null) {
                    map[srcId] = same
                    stats.kept++
                    return
                }
            }
            insert.clearBindings()
            values.forEachIndexed { i, v -> bind(insert, i + 1, v) }
            val newId = insert.executeInsert()
            if (newId < 0) throw SQLiteException("insert into $table failed")
            map[srcId] = newId
            stats.added++
        } catch (e: SQLiteException) {
            stats.skipped++
            map[srcId] = -1L
        }
    }

    /** A row of a table without a uuid that already holds exactly these values (links already rewritten). */
    private fun findIdentical(db: SQLiteDatabase, table: String, names: List<String>, values: List<Any?>): Long? {
        val where = names.joinToString(" AND ") { "\"$it\" IS ?" }
        val statement = db.compileStatement("SELECT id FROM $table WHERE $where LIMIT 1")
        return try {
            values.forEachIndexed { i, v -> bind(statement, i + 1, v) }
            statement.simpleQueryForLong()
        } catch (e: SQLiteDoneException) {
            null
        } finally {
            statement.close()
        }
    }

    private fun read(c: Cursor, i: Int): Any? = when (c.getType(i)) {
        Cursor.FIELD_TYPE_NULL -> null
        Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
        Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
        Cursor.FIELD_TYPE_BLOB -> c.getBlob(i)
        else -> c.getString(i)
    }

    private fun bind(s: android.database.sqlite.SQLiteProgram, i: Int, v: Any?) {
        when (v) {
            null -> s.bindNull(i)
            is Long -> s.bindLong(i, v)
            is Double -> s.bindDouble(i, v)
            is ByteArray -> s.bindBlob(i, v)
            else -> s.bindString(i, v.toString())
        }
    }
}
