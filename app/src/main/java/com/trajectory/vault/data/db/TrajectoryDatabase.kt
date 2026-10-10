package com.trajectory.vault.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    ],
    version = 1,
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

    companion object {
        const val DATABASE_NAME = "Trajectory.db"
        private const val TAG = "Room"

        @Volatile
        private var instance: TrajectoryDatabase? = null

        fun get(context: Context): TrajectoryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrajectoryDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
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
