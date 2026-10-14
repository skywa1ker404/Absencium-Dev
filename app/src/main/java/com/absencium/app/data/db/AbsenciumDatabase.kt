package com.absencium.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.absencium.app.logging.DebugLogger

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
    version = 1,
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

        // Absencium's schema history starts here: version 1 is the first released schema. Every change after it
        // needs a version bump and a Migration added to the list below, never a destructive reset. The schema JSON of
        // each version is exported to app/schemas and checked by CI, so an edit without a version bump fails the build.
        private val MIGRATIONS: Array<Migration> = emptyArray()

        @Volatile
        private var instance: AbsenciumDatabase? = null

        fun get(context: Context): AbsenciumDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AbsenciumDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(*MIGRATIONS).build().also { instance = it }
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
