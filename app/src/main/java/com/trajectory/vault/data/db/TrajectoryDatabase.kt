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
    ],
    version = 2,
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

        /** Version 2 adds the priority column to exams and holidays (Add Calendar Entry's priority selector). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                DebugLogger.i(TAG, "Migrating $DATABASE_NAME from version 1 to 2")
                db.execSQL("ALTER TABLE `Exams_Table` ADD COLUMN `priority` TEXT NOT NULL DEFAULT 'HIGH'")
                db.execSQL("ALTER TABLE `Holidays_Table` ADD COLUMN `priority` TEXT NOT NULL DEFAULT 'LOW'")
                DebugLogger.i(TAG, "Migration 1 to 2 finished")
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
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
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
