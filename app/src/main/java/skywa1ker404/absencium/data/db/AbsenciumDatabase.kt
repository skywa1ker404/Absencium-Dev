package skywa1ker404.absencium.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import skywa1ker404.absencium.logging.DebugLogger

/**
 * The single local database for the app: `Absencium.db`. Its name is a stable identifier (see docs/IDENTITY.md).
 *
 * Upgrades keep the user's data: change an entity, bump [version] and add a Migration (logging through [DebugLogger]).
 * `fallbackToDestructiveMigration` is never used.
 */
@Database(
    entities = [
        SubjectEntity::class,
        AttendanceEntity::class,
        HolidayEntity::class,
        ExamEntity::class,
        TimetableEntity::class,
        NoteEntity::class,
        GuideEntity::class,
        AudioMemoEntity::class,
        RecallQuizEntity::class,
        AiHistoryEntity::class,
        UserStatsEntity::class,
        MomentumLogEntity::class,
        RecallAttemptEntity::class,
        AttachmentEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AbsenciumDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun holidayDao(): HolidayDao
    abstract fun examDao(): ExamDao
    abstract fun timetableDao(): TimetableDao
    abstract fun noteDao(): NoteDao
    abstract fun guideDao(): GuideDao
    abstract fun audioMemoDao(): AudioMemoDao
    abstract fun recallQuizDao(): RecallQuizDao
    abstract fun aiHistoryDao(): AiHistoryDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun momentumLogDao(): MomentumLogDao
    abstract fun recallAttemptDao(): RecallAttemptDao
    abstract fun subjectCascadeDao(): SubjectCascadeDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun entryDao(): EntryDao

    companion object {
        const val DATABASE_NAME = "Absencium.db"
        private const val TAG = "Room"

        // Absencium's schema history: version 1 is the first released schema. Every change after it needs a version bump
        // and a Migration added to the list below, never a destructive reset. The schema JSON of each version is exported
        // to app/schemas and checked by CI, so an edit without a version bump fails the build.
        private val MIGRATIONS: Array<Migration> = arrayOf(Migration2)

        @Volatile
        private var instance: AbsenciumDatabase? = null

        fun get(context: Context): AbsenciumDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AbsenciumDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(*MIGRATIONS)
                    .addCallback(TouchTriggers)
                    .build().also { instance = it }
            }

        /**
         * Opens the database now, which is when Room creates it or runs migrations, and logs the
         * outcome. Open and migration failures are written to debug.log with the full stack trace.
         * Call from a background thread. Failures are logged, not rethrown: the next real query
         * will throw again and surface through the crash handler.
         */
        fun verifyOpen(context: Context) {
            try {
                val database = get(context).openHelper.writableDatabase
                val tables = mutableListOf<String>()
                database.query(
                    "SELECT name FROM sqlite_master WHERE type = 'table' " +
                        "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' " +
                        "AND name != 'room_master_table' ORDER BY name",
                ).use { cursor ->
                    while (cursor.moveToNext()) tables.add(cursor.getString(0))
                }
                DebugLogger.i(
                    TAG,
                    "Database ready: $DATABASE_NAME version=${database.version} " +
                        "tables(${tables.size})=${tables.joinToString()}",
                )
            } catch (t: Throwable) {
                DebugLogger.e(TAG, "Database failed to open or migrate: $DATABASE_NAME", t)
            }
        }

        /** Tables whose rows carry `uuid` and `updated_at`. */
        val TOUCHED_TABLES = listOf(
            "Subjects_Table", "Attendance_Table", "Holidays_Table", "Exams_Table",
            "Timetable_Table", "Notes_Table", "Attachments_Table", "Guides_Table",
        )

        /** Tables whose rows can be moved to the Trash (`deleted_at`). */
        val TRASHABLE_TABLES = listOf("Holidays_Table", "Exams_Table", "Timetable_Table", "Notes_Table")
    }
}

/**
 * Version 2: stable ids, change times, trash marks and the teacher of a lecture. Every statement only adds to the schema
 * or fills the new columns, so existing rows keep all their data.
 */
internal object Migration2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        for (sql in statements()) db.execSQL(sql)
    }

    /** The statements, as plain text so they can also be run against a copy of the version 1 schema outside Android. */
    fun statements(): List<String> {
        val out = mutableListOf<String>()
        val uuidExpr = "lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || substr(hex(randomblob(2)), 2) || '-' || " +
            "substr('89ab', 1 + (abs(random()) % 4), 1) || substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6)))"
        val nowMs = "CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER)"
        for (table in AbsenciumDatabase.TOUCHED_TABLES) {
            out += "ALTER TABLE `$table` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''"
            out += "UPDATE `$table` SET `uuid` = $uuidExpr"
            out += "CREATE UNIQUE INDEX IF NOT EXISTS `index_${table}_uuid` ON `$table` (`uuid`)"
            // Notes already had updated_at in version 1.
            if (table != "Notes_Table") {
                out += "ALTER TABLE `$table` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0"
                out += "UPDATE `$table` SET `updated_at` = $nowMs"
            }
        }
        for (table in AbsenciumDatabase.TRASHABLE_TABLES) {
            out += "ALTER TABLE `$table` ADD COLUMN `deleted_at` INTEGER"
        }
        out += "ALTER TABLE `Timetable_Table` ADD COLUMN `teacher` TEXT"
        return out
    }
}

/**
 * Keeps `updated_at` current without every write having to remember it: after any update that did not itself change
 * `updated_at`, the row gets the current time. Created on every open (idempotent), so a restored database has them too.
 * SQLite does not run a trigger from its own update unless recursive triggers are on, which they are not.
 */
internal object TouchTriggers : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        for (sql in statements()) db.execSQL(sql)
    }

    fun statements(): List<String> = AbsenciumDatabase.TOUCHED_TABLES.map { table ->
        "CREATE TRIGGER IF NOT EXISTS `touch_$table` AFTER UPDATE ON `$table` FOR EACH ROW " +
            "WHEN NEW.`updated_at` = OLD.`updated_at` BEGIN " +
            "UPDATE `$table` SET `updated_at` = CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER) " +
            "WHERE `rowid` = NEW.`rowid`; END"
    }
}
