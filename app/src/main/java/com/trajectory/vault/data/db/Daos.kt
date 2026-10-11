package com.trajectory.vault.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate

@Dao
interface SubjectDao {
    @Insert
    suspend fun insert(subject: SubjectEntity): Long

    @Update
    suspend fun update(subject: SubjectEntity)

    @Delete
    suspend fun delete(subject: SubjectEntity)

    @Query("SELECT * FROM Subjects_Table ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM Subjects_Table WHERE id = :id")
    suspend fun getById(id: Long): SubjectEntity?

    /** Removes the rows the Test Lab adds (names that start with "Sample: "). Returns how many were removed. */
    @Query("DELETE FROM Subjects_Table WHERE name LIKE 'Sample: %'")
    suspend fun deleteSamples(): Int
}

@Dao
interface AttendanceDao {
    @Insert
    suspend fun insert(entry: AttendanceEntity): Long

    @Update
    suspend fun update(entry: AttendanceEntity)

    @Delete
    suspend fun delete(entry: AttendanceEntity)

    @Query("SELECT * FROM Attendance_Table WHERE date = :date ORDER BY id")
    fun observeForDate(date: LocalDate): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM Attendance_Table WHERE subject_id = :subjectId ORDER BY date DESC, id DESC")
    fun observeForSubject(subjectId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT COUNT(*) FROM Attendance_Table WHERE subject_id = :subjectId AND status = :status")
    fun observeCount(subjectId: Long, status: AttendanceStatus): Flow<Int>

    @Query("SELECT * FROM Attendance_Table ORDER BY date, id")
    fun observeAll(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM Attendance_Table WHERE subject_id = :subjectId AND date = :date LIMIT 1")
    suspend fun getFor(subjectId: Long, date: LocalDate): AttendanceEntity?
}

@Dao
interface HolidayDao {
    @Insert
    suspend fun insert(holiday: HolidayEntity): Long

    @Update
    suspend fun update(holiday: HolidayEntity)

    @Delete
    suspend fun delete(holiday: HolidayEntity)

    @Query("SELECT * FROM Holidays_Table ORDER BY date")
    fun observeAll(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM Holidays_Table WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: LocalDate): HolidayEntity?
}

@Dao
interface ExamDao {
    @Insert
    suspend fun insert(exam: ExamEntity): Long

    @Update
    suspend fun update(exam: ExamEntity)

    @Delete
    suspend fun delete(exam: ExamEntity)

    @Query("SELECT * FROM Exams_Table ORDER BY date, id")
    fun observeAll(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM Exams_Table WHERE date >= :from ORDER BY date, id")
    fun observeUpcoming(from: LocalDate): Flow<List<ExamEntity>>

    @Query("SELECT * FROM Exams_Table WHERE date BETWEEN :start AND :end ORDER BY date, id")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<ExamEntity>>

    @Query("SELECT * FROM Exams_Table WHERE id = :id")
    suspend fun getById(id: Long): ExamEntity?
}

@Dao
interface TimetableDao {
    @Insert
    suspend fun insert(slot: TimetableEntity): Long

    @Update
    suspend fun update(slot: TimetableEntity)

    @Delete
    suspend fun delete(slot: TimetableEntity)

    @Query("SELECT * FROM Timetable_Table WHERE day_of_week = :day ORDER BY start_time")
    fun observeForDay(day: DayOfWeek): Flow<List<TimetableEntity>>

    @Query("SELECT * FROM Timetable_Table ORDER BY start_time")
    fun observeAll(): Flow<List<TimetableEntity>>
}

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("SELECT * FROM Notes_Table WHERE subject_id = :subjectId AND status = :status ORDER BY created_date DESC")
    fun observeBySubject(subjectId: Long, status: NoteStatus): Flow<List<NoteEntity>>

    @Query("SELECT * FROM Notes_Table WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?
}

@Dao
interface GuideDao {
    @Insert
    suspend fun insert(guide: GuideEntity): Long

    @Delete
    suspend fun delete(guide: GuideEntity)

    @Query("SELECT * FROM Guides_Table WHERE subject_id = :subjectId ORDER BY file_name COLLATE NOCASE")
    fun observeBySubject(subjectId: Long): Flow<List<GuideEntity>>
}

@Dao
interface AudioMemoDao {
    @Insert
    suspend fun insert(memo: AudioMemoEntity): Long

    @Update
    suspend fun update(memo: AudioMemoEntity)

    @Delete
    suspend fun delete(memo: AudioMemoEntity)

    @Query("SELECT * FROM Audio_Memos_Table WHERE note_id = :noteId ORDER BY id")
    fun observeForNote(noteId: Long): Flow<List<AudioMemoEntity>>
}

@Dao
interface RecallQuizDao {
    @Insert
    suspend fun insert(question: RecallQuizEntity): Long

    @Update
    suspend fun update(question: RecallQuizEntity)

    @Delete
    suspend fun delete(question: RecallQuizEntity)

    @Query("SELECT * FROM Recall_Quiz_Table WHERE next_due_date <= :today ORDER BY next_due_date, id")
    fun observeDue(today: LocalDate): Flow<List<RecallQuizEntity>>

    @Query("SELECT * FROM Recall_Quiz_Table WHERE id = :id")
    suspend fun getById(id: Long): RecallQuizEntity?
}

@Dao
interface AiHistoryDao {
    @Insert
    suspend fun insert(entry: AiHistoryEntity): Long

    @Delete
    suspend fun delete(entry: AiHistoryEntity)

    @Query("SELECT * FROM AI_History_Table ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<AiHistoryEntity>>
}

@Dao
interface UserStatsDao {
    @Insert
    suspend fun insert(stats: UserStatsEntity): Long

    @Update
    suspend fun update(stats: UserStatsEntity)

    @Query("SELECT * FROM User_Stats_Table WHERE date BETWEEN :start AND :end ORDER BY date, id")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<UserStatsEntity>>

    @Query("SELECT COALESCE(SUM(study_duration_minutes), 0) FROM User_Stats_Table WHERE date = :date")
    fun observeMinutesForDate(date: LocalDate): Flow<Int>
}

@Dao
interface MomentumLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MomentumLogEntity)

    @Query("SELECT * FROM Momentum_Log_Table ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MomentumLogEntity>>
}
