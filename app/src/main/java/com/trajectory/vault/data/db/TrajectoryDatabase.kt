package com.trajectory.vault.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.trajectory.vault.logging.DebugLogger

/**
 * The single local database for the app: `Trajectory.db`.
 *
 * Schema export is off for version 1. When the schema first changes, enable it and add a
 * Migration (and log its failures through [DebugLogger]) before bumping [version].
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
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class TrajectoryDatabase : RoomDatabase() {

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
        const val DATABASE_NAME = "Trajectory.db"
        private const val TAG = "Room"

        /** Version 2 adds the priority column to exams and holidays (Add Calendar Entry's priority selector). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                DebugLogger.i(TAG, "Migrating $DATABASE_NAME from version 1 to 2")
                db.execSQL("ALTER TABLE `Exams_Table` ADD COLUMN `priority` TEXT NOT NULL DEFAULT 'HIGH'")
                db.execSQL("ALTER TABLE `Holidays_Table` ADD COLUMN `priority` TEXT NOT NULL DEFAULT 'LOW'")
                DebugLogger.i(TAG, "Migration 1 to 2 finished")
            }
        }

        /**
         * Version 3 adds Attendance_Table.notes_collected (the "did you collect the notes" tracker for
         * absent days) and the Recall_Attempts_Table history of answered recall questions.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                DebugLogger.i(TAG, "Migrating $DATABASE_NAME from version 2 to 3")
                db.execSQL("ALTER TABLE `Attendance_Table` ADD COLUMN `notes_collected` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Recall_Attempts_Table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`question_id` INTEGER NOT NULL, `note_id` INTEGER, `prompt_text` TEXT NOT NULL, " +
                        "`correct` INTEGER NOT NULL, `answered_at` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_Recall_Attempts_Table_answered_at` " +
                        "ON `Recall_Attempts_Table` (`answered_at`)",
                )
                DebugLogger.i(TAG, "Migration 2 to 3 finished")
            }
        }

        /**
         * Version 4 adds the entry kind, pin, tags, written-on-copy flag, absent date and edit time to
         * Notes_Table, and the Attachments_Table that holds photos, documents, recordings and sketches.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                DebugLogger.i(TAG, "Migrating $DATABASE_NAME from version 3 to 4")
                db.execSQL("ALTER TABLE `Notes_Table` ADD COLUMN `kind` TEXT NOT NULL DEFAULT 'TEXT'")
                db.execSQL("ALTER TABLE `Notes_Table` ADD COLUMN `pinned` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `Notes_Table` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `Notes_Table` ADD COLUMN `written_on_copy` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `Notes_Table` ADD COLUMN `written_at` INTEGER")
                db.execSQL("ALTER TABLE `Notes_Table` ADD COLUMN `absent_date` TEXT")
                db.execSQL("ALTER TABLE `Notes_Table` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `Notes_Table` SET `updated_at` = `created_date`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Attachments_Table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`note_id` INTEGER NOT NULL, `kind` TEXT NOT NULL, `file_path` TEXT NOT NULL, " +
                        "`display_name` TEXT NOT NULL, `mime_type` TEXT NOT NULL, `size_bytes` INTEGER NOT NULL, " +
                        "`extracted_text` TEXT NOT NULL DEFAULT '', `duration_ms` INTEGER NOT NULL DEFAULT 0, " +
                        "`created_at` INTEGER NOT NULL DEFAULT 0, " +
                        "FOREIGN KEY(`note_id`) REFERENCES `Notes_Table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_Attachments_Table_note_id` ON `Attachments_Table` (`note_id`)")
                DebugLogger.i(TAG, "Migration 3 to 4 finished")
            }
        }

        @Volatile
        private var instance: TrajectoryDatabase? = null

        fun get(context: Context): TrajectoryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrajectoryDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
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
    }
}
