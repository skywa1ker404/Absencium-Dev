package com.trajectory.vault.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

// Enums are stored as their name (TEXT). Dates are stored as ISO-8601 text
// (see Converters), so ordering by a date column sorts chronologically.

enum class AttendanceStatus { PRESENT, ABSENT, CANCELLED }

enum class NoteStatus { ACTIVE, FINISHED }

enum class GuideType { QP, SYLLABUS, ANSWER_BANK }

/**
 * Not listed in the spec's table list, but every other table references a subject and the
 * Settings hub needs per-subject active/muted state, so subjects need a home.
 */
@Entity(
    tableName = "Subjects_Table",
    indices = [Index(value = ["name"], unique = true)],
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "is_muted") val isMuted: Boolean = false,
)

@Entity(
    tableName = "Attendance_Table",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subject_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("subject_id"), Index("date")],
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "subject_id") val subjectId: Long,
    val date: LocalDate,
    val status: AttendanceStatus,
    val notes: String? = null,
)

@Entity(
    tableName = "Holidays_Table",
    indices = [Index("date")],
)
data class HolidayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val title: String,
    @ColumnInfo(name = "auto_skip_flag") val autoSkip: Boolean = true,
)

@Entity(
    tableName = "Exams_Table",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subject_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("subject_id"), Index("date")],
)
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "subject_id") val subjectId: Long,
    val date: LocalDate,
    val title: String,
    val weightage: Double? = null,
    val room: String? = null,
    @ColumnInfo(name = "syllabus_notes") val syllabusNotes: String? = null,
)

@Entity(
    tableName = "Timetable_Table",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subject_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("subject_id"), Index("day_of_week")],
)
data class TimetableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "day_of_week") val dayOfWeek: DayOfWeek,
    @ColumnInfo(name = "subject_id") val subjectId: Long,
    @ColumnInfo(name = "start_time") val startTime: LocalTime,
    @ColumnInfo(name = "end_time") val endTime: LocalTime,
    @ColumnInfo(name = "room_number") val roomNumber: String? = null,
)

@Entity(
    tableName = "Notes_Table",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subject_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("subject_id"), Index("status")],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "subject_id") val subjectId: Long,
    val title: String,
    @ColumnInfo(name = "body_text") val bodyText: String,
    val status: NoteStatus = NoteStatus.ACTIVE,
    /** Epoch milliseconds. */
    @ColumnInfo(name = "created_date") val createdDate: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "Guides_Table",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subject_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("subject_id")],
)
data class GuideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "subject_id") val subjectId: Long,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "local_uri") val localUri: String,
    @ColumnInfo(name = "guide_type") val guideType: GuideType,
)

@Entity(
    tableName = "Audio_Memos_Table",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("note_id")],
)
data class AudioMemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "note_id") val noteId: Long,
    @ColumnInfo(name = "file_path") val filePath: String,
    /** Recording length in milliseconds. */
    @ColumnInfo(name = "duration") val durationMs: Long,
    /** JSON array of bookmark timestamps. */
    @ColumnInfo(name = "timestamps_json") val timestampsJson: String = "[]",
)

@Entity(
    tableName = "Recall_Quiz_Table",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("note_id"), Index("next_due_date")],
)
data class RecallQuizEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "note_id") val noteId: Long,
    @ColumnInfo(name = "prompt_text") val promptText: String,
    @ColumnInfo(name = "correct_answer") val correctAnswer: String,
    @ColumnInfo(name = "last_answered_date") val lastAnsweredDate: LocalDate? = null,
    @ColumnInfo(name = "next_due_date") val nextDueDate: LocalDate,
    @ColumnInfo(name = "is_repeat") val isRepeat: Boolean = false,
    @ColumnInfo(name = "streak_count") val streakCount: Int = 0,
)

/** History is kept when its source note is deleted; the link is cleared instead. */
@Entity(
    tableName = "AI_History_Table",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("note_id")],
)
data class AiHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "note_id") val noteId: Long? = null,
    @ColumnInfo(name = "selected_text") val selectedText: String,
    @ColumnInfo(name = "ai_response") val aiResponse: String,
    /** Epoch milliseconds. */
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * One row per study session so each session can carry its own category tag.
 * Daily totals are the sum of a date's rows.
 */
@Entity(
    tableName = "User_Stats_Table",
    indices = [Index("date")],
)
data class UserStatsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    @ColumnInfo(name = "study_duration_minutes") val studyDurationMinutes: Int,
    @ColumnInfo(name = "streak_achieved") val streakAchieved: Boolean = false,
    @ColumnInfo(name = "category_tag") val categoryTag: String? = null,
)

@Entity(tableName = "Momentum_Log_Table")
data class MomentumLogEntity(
    @PrimaryKey val date: LocalDate,
    @ColumnInfo(name = "focus_quality_score") val focusQualityScore: Double,
    @ColumnInfo(name = "rolling_7day_consistency") val rolling7DayConsistency: Double,
)
