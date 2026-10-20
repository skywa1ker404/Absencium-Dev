package skywa1ker404.absencium.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

// Enums are stored as their name (TEXT). Dates are stored as ISO-8601 text
// (see Converters), so ordering by a date column sorts chronologically.

enum class AttendanceStatus { PRESENT, ABSENT, CANCELLED }

/** DRAFT is a note that was saved from the Create New Entry screen but is not filed in its folder yet. */
enum class NoteStatus { ACTIVE, FINISHED, DRAFT }

/** How loudly an exam or holiday is surfaced. High is the exam alert, Low is a plain holiday. */
enum class Priority { HIGH, NORMAL, LOW }

enum class GuideType { QP, SYLLABUS, ANSWER_BANK }

/** What a vault entry was created from. Every entry can hold text and attachments whatever its kind. */
enum class NoteKind { TEXT, PICTURES, DOCUMENT, AUDIO, HOMEWORK, AI_NOTES }

enum class AttachmentKind { IMAGE, DOCUMENT, AUDIO, SKETCH }

/**
 * Not listed in the spec's table list, but every other table references a subject and the
 * Settings hub needs per-subject active/muted state, so subjects need a home.
 */
@Entity(
    tableName = "Subjects_Table",
    indices = [Index(value = ["name"], unique = true), Index(value = ["uuid"], unique = true)],
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "is_muted") val isMuted: Boolean = false,
    /** Stable identity that survives export, import and (later) sync; the numeric id is local to one database. */
    @ColumnInfo(defaultValue = "''") val uuid: String = UUID.randomUUID().toString(),
    /** Epoch milliseconds of the last change; kept current by a database trigger (see AbsenciumDatabase). */
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = System.currentTimeMillis(),
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
    indices = [Index("subject_id"), Index("date"), Index(value = ["uuid"], unique = true)],
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "subject_id") val subjectId: Long,
    val date: LocalDate,
    val status: AttendanceStatus,
    val notes: String? = null,
    /** For an absent day: whether the notes that were taught that day have been collected. */
    @ColumnInfo(name = "notes_collected", defaultValue = "0") val notesCollected: Boolean = false,
    /** Stable identity that survives export, import and (later) sync; the numeric id is local to one database. */
    @ColumnInfo(defaultValue = "''") val uuid: String = UUID.randomUUID().toString(),
    /** Epoch milliseconds of the last change; kept current by a database trigger (see AbsenciumDatabase). */
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "Holidays_Table",
    indices = [Index("date"), Index(value = ["uuid"], unique = true)],
)
data class HolidayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val title: String,
    @ColumnInfo(name = "auto_skip_flag") val autoSkip: Boolean = true,
    @ColumnInfo(defaultValue = "'LOW'") val priority: Priority = Priority.LOW,
    /** Stable identity that survives export, import and (later) sync; the numeric id is local to one database. */
    @ColumnInfo(defaultValue = "''") val uuid: String = UUID.randomUUID().toString(),
    /** Epoch milliseconds of the last change; kept current by a database trigger (see AbsenciumDatabase). */
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = System.currentTimeMillis(),
    /** Epoch milliseconds when the row was moved to the Trash, or null while it is live. */
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
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
    indices = [Index("subject_id"), Index("date"), Index(value = ["uuid"], unique = true)],
)
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "subject_id") val subjectId: Long,
    val date: LocalDate,
    val title: String,
    val weightage: Double? = null,
    val room: String? = null,
    @ColumnInfo(name = "syllabus_notes") val syllabusNotes: String? = null,
    @ColumnInfo(defaultValue = "'HIGH'") val priority: Priority = Priority.HIGH,
    /** Stable identity that survives export, import and (later) sync; the numeric id is local to one database. */
    @ColumnInfo(defaultValue = "''") val uuid: String = UUID.randomUUID().toString(),
    /** Epoch milliseconds of the last change; kept current by a database trigger (see AbsenciumDatabase). */
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = System.currentTimeMillis(),
    /** Epoch milliseconds when the row was moved to the Trash, or null while it is live. */
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
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
    indices = [Index("subject_id"), Index("day_of_week"), Index(value = ["uuid"], unique = true)],
)
data class TimetableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "day_of_week") val dayOfWeek: DayOfWeek,
    @ColumnInfo(name = "subject_id") val subjectId: Long,
    @ColumnInfo(name = "start_time") val startTime: LocalTime,
    @ColumnInfo(name = "end_time") val endTime: LocalTime,
    @ColumnInfo(name = "room_number") val roomNumber: String? = null,
    /** The teacher of the lecture, shown on the timetable. */
    val teacher: String? = null,
    /** Stable identity that survives export, import and (later) sync; the numeric id is local to one database. */
    @ColumnInfo(defaultValue = "''") val uuid: String = UUID.randomUUID().toString(),
    /** Epoch milliseconds of the last change; kept current by a database trigger (see AbsenciumDatabase). */
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = System.currentTimeMillis(),
    /** Epoch milliseconds when the row was moved to the Trash, or null while it is live. */
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
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
    indices = [Index("subject_id"), Index("status"), Index(value = ["uuid"], unique = true)],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "subject_id") val subjectId: Long,
    val title: String,
    @ColumnInfo(name = "body_text") val bodyText: String,
    val status: NoteStatus = NoteStatus.ACTIVE,
    /** Epoch milliseconds. */
    @ColumnInfo(name = "created_date") val createdDate: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'TEXT'") val kind: NoteKind = NoteKind.TEXT,
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
    /** Comma separated tags. */
    @ColumnInfo(defaultValue = "''") val tags: String = "",
    /** Set once the notes have been written out on the student's own copy. */
    @ColumnInfo(name = "written_on_copy", defaultValue = "0") val writtenOnCopy: Boolean = false,
    /** Epoch milliseconds of when [writtenOnCopy] was set. */
    @ColumnInfo(name = "written_at") val writtenAt: Long? = null,
    /** The absent day these notes were collected for, if any. */
    @ColumnInfo(name = "absent_date") val absentDate: LocalDate? = null,
    /** Epoch milliseconds of the last edit. */
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = 0L,
    /** Stable identity that survives export, import and (later) sync; the numeric id is local to one database. */
    @ColumnInfo(defaultValue = "''") val uuid: String = UUID.randomUUID().toString(),
    /** Epoch milliseconds when the row was moved to the Trash, or null while it is live. */
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

/** A photo, document, recording or sketch that belongs to an entry. The file lives in the app's own storage. */
@Entity(
    tableName = "Attachments_Table",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("note_id"), Index(value = ["uuid"], unique = true)],
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "note_id") val noteId: Long,
    val kind: AttachmentKind,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    /** Text read from the file by AI (photos, documents) or the voice-to-text transcript (recordings). */
    @ColumnInfo(name = "extracted_text", defaultValue = "''") val extractedText: String = "",
    /** Length of a recording in milliseconds; 0 for everything else. */
    @ColumnInfo(name = "duration_ms", defaultValue = "0") val durationMs: Long = 0L,
    /** Epoch milliseconds. */
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = System.currentTimeMillis(),
    /** Stable identity that survives export, import and (later) sync; the numeric id is local to one database. */
    @ColumnInfo(defaultValue = "''") val uuid: String = UUID.randomUUID().toString(),
    /** Epoch milliseconds of the last change; kept current by a database trigger (see AbsenciumDatabase). */
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = System.currentTimeMillis(),
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
    indices = [Index("subject_id"), Index(value = ["uuid"], unique = true)],
)
data class GuideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "subject_id") val subjectId: Long,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "local_uri") val localUri: String,
    @ColumnInfo(name = "guide_type") val guideType: GuideType,
    /** Stable identity that survives export, import and (later) sync; the numeric id is local to one database. */
    @ColumnInfo(defaultValue = "''") val uuid: String = UUID.randomUUID().toString(),
    /** Epoch milliseconds of the last change; kept current by a database trigger (see AbsenciumDatabase). */
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = System.currentTimeMillis(),
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

/** One answered Daily Recall question, kept so the history log can show right and wrong answers per topic. */
@Entity(
    tableName = "Recall_Attempts_Table",
    indices = [Index("answered_at")],
)
data class RecallAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "question_id") val questionId: Long,
    /** The note the question came from. Not a foreign key, so the history survives the note being deleted. */
    @ColumnInfo(name = "note_id") val noteId: Long? = null,
    @ColumnInfo(name = "prompt_text") val promptText: String,
    val correct: Boolean,
    /** Epoch milliseconds. */
    @ColumnInfo(name = "answered_at") val answeredAt: Long = System.currentTimeMillis(),
)
