package skywa1ker404.absencium.data.ai

import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import me.rerere.ai.core.InputSchema
import me.rerere.ai.core.Tool
import me.rerere.ai.core.ToolApprovalMode
import skywa1ker404.absencium.AppContainer
import skywa1ker404.absencium.data.db.AttendanceStatus
import skywa1ker404.absencium.data.db.NoteStatus
import skywa1ker404.absencium.data.db.Priority
import skywa1ker404.absencium.data.db.SubjectEntity
import skywa1ker404.absencium.data.db.SyllabusStatus
import skywa1ker404.absencium.logging.DebugLogger
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/**
 * What the embedded Absencium AI chat can do inside the app: look things up (subjects, timetable, exams, holidays, attendance, syllabus,
 * notes) and create, change or delete them. It goes through the same repositories the screens use, so a deleted item lands in the Trash
 * and everything can be undone the same way. Looking things up runs by itself; every change, and reading a whole note, waits for the
 * student to approve it in the chat. Each lookup by name must match exactly one thing, otherwise the tool says so instead of guessing.
 */
class ChatAppTools(private val container: AppContainer) {

    private companion object {
        const val APP_PROMPT = """You are Absencium AI, the assistant built into Absencium, the student's offline college companion app (attendance, timetable, exams, holidays, notes, syllabus, Vault).
You can look things up and change them with the app tools. Look the answer up with a tool instead of guessing, and never say something was added, changed or deleted unless the tool answered ok. Changes wait for the student's approval in the chat. Deleted items go to the Trash and can be restored. Dates are YYYY-MM-DD and times HH:mm. Keep answers short and plain."""
    }

    fun tools(): List<Tool> = listOf(
        // Looking things up
        read("app_overview", "Today's date, the subjects, the lectures today, upcoming exams and the attendance percentage per subject. Call this first when the question is about the student's college life.", emptyMap()) { overview() }.copy(systemPrompt = { _, _ -> APP_PROMPT }),
        read("list_subjects", "All subjects.", emptyMap()) { subjectsJson() },
        read("get_timetable", "The weekly timetable. Optional day (MONDAY..SUNDAY) to get one day only.", mapOf("day" to text("MONDAY..SUNDAY"))) { a -> timetable(a.str("day")) },
        read("list_exams", "Exams with dates and rooms. upcoming_only defaults to true.", mapOf("upcoming_only" to bool("Only exams from today on"))) { a -> exams(a.bool("upcoming_only") ?: true) },
        read("list_holidays", "Holidays from today on, or all of them when all is true.", mapOf("all" to bool("Include past holidays"))) { a -> holidays(a.bool("all") ?: false) },
        read("get_attendance", "Attendance counts (present, absent, cancelled, percentage) for one subject or all, and its last 15 marked days.", mapOf("subject" to text("Subject name; leave out for all"))) { a -> attendance(a.str("subject")) },
        read("get_syllabus", "The syllabus topics of one subject with their progress.", mapOf("subject" to text("Subject name")), required = listOf("subject")) { a -> syllabus(a.req("subject")) },
        read("search_notes", "Searches note titles and the start of note text. Returns id, subject, title and a short snippet.", mapOf("query" to text("Words to look for"), "subject" to text("Optional subject name")), required = listOf("query")) { a -> searchNotes(a.req("query"), a.str("subject")) },
        read("search_app", "Searches everything by words: subjects, notes, exams, holidays and syllabus topics.", mapOf("query" to text("Words to look for")), required = listOf("query")) { a -> searchApp(a.req("query")) },
        write("read_note", "Reads the whole text of one note (the student approves each time, because it is sent to the AI provider).", mapOf("note_id" to num("Note id from search_notes")), required = listOf("note_id"), needsApproval = true) { a -> readNote(a.long("note_id")) },

        // Creating and changing
        write("add_subject", "Adds a subject.", mapOf("name" to text("Subject name")), required = listOf("name")) { a -> subjectResult(container.subjectRepository.add(a.req("name")), "Added the subject.") },
        write("rename_subject", "Renames a subject.", mapOf("subject" to text("Current name"), "new_name" to text("New name")), required = listOf("subject", "new_name")) { a ->
            val s = subject(a.req("subject")) ?: return@write missing(a.req("subject"))
            subjectResult(container.subjectRepository.rename(s, a.req("new_name")), "Renamed.")
        },
        write("delete_subject", "Deletes a subject. It is refused while the subject still has entries.", mapOf("subject" to text("Subject name")), required = listOf("subject")) { a ->
            val s = subject(a.req("subject")) ?: return@write missing(a.req("subject"))
            subjectResult(container.subjectRepository.delete(s), "Deleted the subject.")
        },
        write("add_note", "Adds a note to a subject.", mapOf("subject" to text("Subject name"), "title" to text("Note title"), "body" to text("Note text")), required = listOf("subject", "title", "body")) { a ->
            val s = subject(a.req("subject")) ?: return@write missing(a.req("subject"))
            container.vaultRepository.saveNote(0, s.id, a.req("title"), a.req("body"), NoteStatus.ACTIVE)
            ok("Added the note to ${s.name}.")
        },
        write("update_note", "Changes the title and/or text of a note. append_text is added to the end of the existing text.", mapOf("note_id" to num("Note id"), "title" to text("New title"), "body" to text("New full text (replaces the old text)"), "append_text" to text("Text to add at the end")), required = listOf("note_id")) { a ->
            val note = container.vaultRepository.getNote(a.long("note_id") ?: return@write fail("note_id is missing.")) ?: return@write fail("No such note.")
            val body = a.str("body") ?: (note.bodyText + (a.str("append_text")?.let { "\n$it" } ?: ""))
            container.vaultRepository.saveNote(note.id, note.subjectId, a.str("title") ?: note.title, body, note.status, note.tags)
            ok("Note updated.")
        },
        write("delete_note", "Moves a note to the Trash.", mapOf("note_id" to num("Note id")), required = listOf("note_id")) { a ->
            val note = container.vaultRepository.getNote(a.long("note_id") ?: return@write fail("note_id is missing.")) ?: return@write fail("No such note.")
            container.vaultRepository.deleteNote(note)
            ok("Moved the note to the Trash.")
        },
        write("add_timetable_slot", "Adds a weekly lecture.", mapOf("subject" to text("Subject name"), "day" to text("MONDAY..SUNDAY"), "start" to text("HH:mm"), "end" to text("HH:mm"), "room" to text("Room, optional")), required = listOf("subject", "day", "start", "end")) { a ->
            val s = subject(a.req("subject")) ?: return@write missing(a.req("subject"))
            val start = time(a.req("start")) ?: return@write fail("Start must look like 09:30.")
            val end = time(a.req("end")) ?: return@write fail("End must look like 10:30.")
            if (!end.isAfter(start)) return@write fail("The lecture must end after it starts.")
            container.calendarRepository.addSlot(day(a.req("day")) ?: return@write fail("Unknown day."), s.id, start, end, a.str("room"))
            ok("Added to the timetable.")
        },
        write("update_timetable_slot", "Moves or edits a lecture. Identify it by subject and day (and start when the subject has several that day).", mapOf("subject" to text("Subject name"), "day" to text("Current day"), "start" to text("Current start HH:mm, only if needed"), "new_day" to text("New day"), "new_start" to text("New start HH:mm"), "new_end" to text("New end HH:mm"), "room" to text("New room")), required = listOf("subject", "day")) { a ->
            val slot = slot(a) ?: return@write fail("No single lecture matches. Say the subject, day and start time.")
            val changed = slot.copy(
                dayOfWeek = a.str("new_day")?.let { day(it) } ?: slot.dayOfWeek,
                startTime = a.str("new_start")?.let { time(it) } ?: slot.startTime,
                endTime = a.str("new_end")?.let { time(it) } ?: slot.endTime,
                roomNumber = a.str("room") ?: slot.roomNumber,
            )
            if (!changed.endTime.isAfter(changed.startTime)) return@write fail("The lecture must end after it starts.")
            container.calendarRepository.updateSlot(changed)
            ok("Timetable changed.")
        },
        write("delete_timetable_slot", "Moves a lecture to the Trash.", mapOf("subject" to text("Subject name"), "day" to text("Day"), "start" to text("Start HH:mm, only if needed")), required = listOf("subject", "day")) { a ->
            val slot = slot(a) ?: return@write fail("No single lecture matches. Say the subject, day and start time.")
            container.calendarRepository.deleteSlot(slot)
            ok("Moved the lecture to the Trash.")
        },
        write("add_exam", "Adds an exam.", mapOf("subject" to text("Subject name"), "title" to text("Exam title"), "date" to text("YYYY-MM-DD"), "room" to text("Room"), "weightage" to num("Weightage percent")), required = listOf("subject", "title", "date")) { a ->
            val s = subject(a.req("subject")) ?: return@write missing(a.req("subject"))
            container.calendarRepository.addExam(s.id, date(a.req("date")) ?: return@write fail("Date must look like 2026-12-05."), a.req("title"), a.dbl("weightage"), a.str("room"), null, Priority.HIGH)
            ok("Exam added.")
        },
        write("update_exam", "Changes an exam found by words in its title or subject.", mapOf("match" to text("Words from the exam title or subject"), "title" to text("New title"), "date" to text("New date YYYY-MM-DD"), "room" to text("New room")), required = listOf("match")) { a ->
            val exam = exam(a.req("match")) ?: return@write fail("No single exam matches \"${a.req("match")}\".")
            container.calendarRepository.updateExam(exam.copy(title = a.str("title") ?: exam.title, date = a.str("date")?.let { date(it) } ?: exam.date, room = a.str("room") ?: exam.room))
            ok("Exam changed.")
        },
        write("delete_exam", "Moves an exam to the Trash.", mapOf("match" to text("Words from the exam title or subject")), required = listOf("match")) { a ->
            val exam = exam(a.req("match")) ?: return@write fail("No single exam matches \"${a.req("match")}\".")
            container.calendarRepository.deleteExam(exam)
            ok("Moved the exam to the Trash.")
        },
        write("add_holiday", "Adds a holiday.", mapOf("date" to text("YYYY-MM-DD"), "title" to text("Holiday name")), required = listOf("date", "title")) { a ->
            val d = date(a.req("date")) ?: return@write fail("Date must look like 2026-12-25.")
            if (container.database.holidayDao().getForDate(d) != null) return@write fail("That day already has a holiday.")
            container.calendarRepository.addHoliday(d, a.req("title"), true, Priority.LOW)
            ok("Holiday added.")
        },
        write("delete_holiday", "Moves the holiday on a date to the Trash.", mapOf("date" to text("YYYY-MM-DD")), required = listOf("date")) { a ->
            val h = container.database.holidayDao().getForDate(date(a.req("date")) ?: return@write fail("Date must look like 2026-12-25.")) ?: return@write fail("No holiday on that day.")
            container.calendarRepository.deleteHoliday(h)
            ok("Moved the holiday to the Trash.")
        },
        write("set_attendance", "Marks a subject PRESENT, ABSENT or CANCELLED on a date.", mapOf("subject" to text("Subject name"), "date" to text("YYYY-MM-DD"), "status" to text("PRESENT, ABSENT or CANCELLED")), required = listOf("subject", "date", "status")) { a ->
            val s = subject(a.req("subject")) ?: return@write missing(a.req("subject"))
            val status = runCatching { AttendanceStatus.valueOf(a.req("status").trim().uppercase()) }.getOrNull() ?: return@write fail("Status must be PRESENT, ABSENT or CANCELLED.")
            container.calendarRepository.setAttendance(s.id, date(a.req("date")) ?: return@write fail("Date must look like 2026-12-05."), status)
            ok("Attendance marked.")
        },
        write("set_syllabus_status", "Marks a syllabus topic NOT_STARTED, IN_PROGRESS or COMPLETED.", mapOf("subject" to text("Subject name"), "topic" to text("Topic title"), "status" to text("NOT_STARTED, IN_PROGRESS or COMPLETED")), required = listOf("subject", "topic", "status")) { a ->
            val s = subject(a.req("subject")) ?: return@write missing(a.req("subject"))
            val status = runCatching { SyllabusStatus.valueOf(a.req("status").trim().uppercase()) }.getOrNull() ?: return@write fail("Status must be NOT_STARTED, IN_PROGRESS or COMPLETED.")
            val nodes = container.database.syllabusDao().observeAll().first().filter { it.subjectId == s.id }
            val topic = a.req("topic").trim()
            val matches = nodes.filter { it.title.equals(topic, true) }.ifEmpty { nodes.filter { it.title.contains(topic, true) } }
            val node = matches.singleOrNull() ?: return@write fail(if (matches.isEmpty()) "No syllabus topic matches \"$topic\"." else "Several topics match \"$topic\".")
            container.database.syllabusDao().update(node.copy(status = status, updatedAt = System.currentTimeMillis()))
            ok("Syllabus updated.")
        },
    )

    // ---- lookups ----

    private suspend fun overview(): JsonElement {
        val data = container.calendarRepository.data.first()
        val today = LocalDate.now()
        val names = data.subjects.associate { it.id to it.name }
        return buildJsonObject {
            put("today", today.toString())
            put("weekday", today.dayOfWeek.name)
            put("subjects", JsonArray(data.subjects.map { JsonPrimitive(it.name) }))
            put("lectures_today", buildJsonArray {
                data.timetable.filter { it.dayOfWeek == today.dayOfWeek }.sortedBy { it.startTime }.forEach { add(slotJson(it, names)) }
            })
            put("upcoming_exams", buildJsonArray {
                data.exams.filter { !it.date.isBefore(today) }.sortedBy { it.date }.take(8).forEach { add(examJson(it, names)) }
            })
            put("attendance", buildJsonArray {
                data.subjects.forEach { s -> add(attendanceJson(s, data.attendance.filter { it.subjectId == s.id })) }
            })
        }
    }

    private suspend fun subjectsJson(): JsonElement = buildJsonObject {
        put("subjects", buildJsonArray {
            container.database.subjectDao().observeAll().first().forEach { s ->
                add(buildJsonObject { put("name", s.name); put("active", s.isActive); put("muted", s.isMuted) })
            }
        })
    }

    private suspend fun timetable(dayName: String?): JsonElement {
        val data = container.calendarRepository.data.first()
        val names = data.subjects.associate { it.id to it.name }
        val wanted = dayName?.let { day(it) ?: return fail("Unknown day.") }
        return buildJsonObject {
            put("lectures", buildJsonArray {
                data.timetable.filter { wanted == null || it.dayOfWeek == wanted }
                    .sortedWith(compareBy({ it.dayOfWeek }, { it.startTime })).forEach { add(slotJson(it, names)) }
            })
        }
    }

    private suspend fun exams(upcomingOnly: Boolean): JsonElement {
        val data = container.calendarRepository.data.first()
        val names = data.subjects.associate { it.id to it.name }
        val today = LocalDate.now()
        return buildJsonObject {
            put("exams", buildJsonArray {
                data.exams.filter { !upcomingOnly || !it.date.isBefore(today) }.sortedBy { it.date }.forEach { add(examJson(it, names)) }
            })
        }
    }

    private suspend fun holidays(all: Boolean): JsonElement {
        val today = LocalDate.now()
        return buildJsonObject {
            put("holidays", buildJsonArray {
                container.calendarRepository.data.first().holidays.filter { all || !it.date.isBefore(today) }.sortedBy { it.date }
                    .forEach { h -> add(buildJsonObject { put("date", h.date.toString()); put("title", h.title) }) }
            })
        }
    }

    private suspend fun attendance(subjectName: String?): JsonElement {
        val data = container.calendarRepository.data.first()
        val chosen = if (subjectName == null) data.subjects else listOf(subject(subjectName) ?: return missing(subjectName))
        return buildJsonObject {
            put("attendance", buildJsonArray {
                chosen.forEach { s ->
                    val rows = data.attendance.filter { it.subjectId == s.id }
                    add(buildJsonObject {
                        attendanceJson(s, rows).forEach { (k, v) -> put(k, v) }
                        put("recent", buildJsonArray {
                            rows.sortedByDescending { it.date }.take(15).forEach { r -> add(buildJsonObject { put("date", r.date.toString()); put("status", r.status.name) }) }
                        })
                    })
                }
            })
        }
    }

    private suspend fun syllabus(subjectName: String): JsonElement {
        val s = subject(subjectName) ?: return missing(subjectName)
        val nodes = container.database.syllabusDao().observeAll().first().filter { it.subjectId == s.id }
        val byId = nodes.associateBy { it.id }
        return buildJsonObject {
            put("subject", s.name)
            put("topics", buildJsonArray {
                nodes.sortedWith(compareBy({ it.parentId ?: 0L }, { it.position })).forEach { n ->
                    add(buildJsonObject {
                        put("title", n.title)
                        put("status", n.status.name)
                        n.parentId?.let { byId[it]?.title }?.let { put("under", it) }
                    })
                }
            })
        }
    }

    private suspend fun searchNotes(query: String, subjectName: String?): JsonElement {
        val names = container.database.subjectDao().observeAll().first().associate { it.id to it.name }
        val only = subjectName?.let { (subject(it) ?: return missing(it)).id }
        val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        return buildJsonObject {
            put("notes", buildJsonArray {
                container.database.noteDao().observeAll().first()
                    .filter { only == null || it.subjectId == only }
                    .filter { n -> words.all { w -> n.title.lowercase().contains(w) || n.bodyText.lowercase().contains(w) } }
                    .take(15).forEach { n ->
                        add(buildJsonObject {
                            put("id", n.id)
                            put("subject", names[n.subjectId].orEmpty())
                            put("title", n.title)
                            put("snippet", n.bodyText.replace(Regex("\\s+"), " ").take(140))
                        })
                    }
            })
        }
    }

    private suspend fun searchApp(query: String): JsonElement {
        val q = query.trim().lowercase()
        val data = container.calendarRepository.data.first()
        val names = data.subjects.associate { it.id to it.name }
        return buildJsonObject {
            put("subjects", JsonArray(data.subjects.filter { it.name.lowercase().contains(q) }.map { JsonPrimitive(it.name) }))
            put("exams", buildJsonArray { data.exams.filter { it.title.lowercase().contains(q) || names[it.subjectId].orEmpty().lowercase().contains(q) }.forEach { add(examJson(it, names)) } })
            put("holidays", buildJsonArray { data.holidays.filter { it.title.lowercase().contains(q) }.forEach { h -> add(buildJsonObject { put("date", h.date.toString()); put("title", h.title) }) } })
            put("syllabus_topics", buildJsonArray {
                container.database.syllabusDao().observeAll().first().filter { it.title.lowercase().contains(q) }.take(15)
                    .forEach { n -> add(buildJsonObject { put("subject", names[n.subjectId].orEmpty()); put("title", n.title); put("status", n.status.name) }) }
            })
            put("notes", (searchNotes(query, null) as JsonObject)["notes"] ?: JsonArray(emptyList()))
        }
    }

    private suspend fun readNote(id: Long?): JsonElement {
        val note = container.vaultRepository.getNote(id ?: return fail("note_id is missing.")) ?: return fail("No such note.")
        return buildJsonObject { put("title", note.title); put("text", note.bodyText.take(20_000)) }
    }

    // ---- helpers ----

    private fun attendanceJson(s: SubjectEntity, rows: List<skywa1ker404.absencium.data.db.AttendanceEntity>): JsonObject {
        val present = rows.count { it.status == AttendanceStatus.PRESENT }
        val absent = rows.count { it.status == AttendanceStatus.ABSENT }
        val cancelled = rows.count { it.status == AttendanceStatus.CANCELLED }
        val counted = present + absent
        return buildJsonObject {
            put("subject", s.name)
            put("present", present)
            put("absent", absent)
            put("cancelled", cancelled)
            put("percentage", if (counted == 0) JsonPrimitive("no classes counted yet") else JsonPrimitive(String.format(Locale.US, "%.1f", present * 100.0 / counted)))
        }
    }

    private fun slotJson(t: skywa1ker404.absencium.data.db.TimetableEntity, names: Map<Long, String>) = buildJsonObject {
        put("subject", names[t.subjectId].orEmpty())
        put("day", t.dayOfWeek.name)
        put("start", t.startTime.toString())
        put("end", t.endTime.toString())
        t.roomNumber?.let { put("room", it) }
    }

    private fun examJson(e: skywa1ker404.absencium.data.db.ExamEntity, names: Map<Long, String>) = buildJsonObject {
        put("subject", names[e.subjectId].orEmpty())
        put("title", e.title)
        put("date", e.date.toString())
        e.room?.let { put("room", it) }
        e.weightage?.let { put("weightage", it) }
    }

    private suspend fun subject(name: String): SubjectEntity? {
        val all = container.database.subjectDao().observeAll().first()
        return all.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
            ?: all.filter { it.name.contains(name.trim(), ignoreCase = true) }.singleOrNull()
    }

    private suspend fun slot(a: JsonObject): skywa1ker404.absencium.data.db.TimetableEntity? {
        val s = subject(a.req("subject")) ?: return null
        val d = day(a.req("day")) ?: return null
        val start = a.str("start")?.let { time(it) }
        return container.database.timetableDao().observeAll().first()
            .filter { it.subjectId == s.id && it.dayOfWeek == d && (start == null || it.startTime == start) }.singleOrNull()
    }

    private suspend fun exam(match: String): skywa1ker404.absencium.data.db.ExamEntity? {
        val names = container.database.subjectDao().observeAll().first().associate { it.id to it.name }
        return container.database.examDao().observeAll().first()
            .filter { it.title.contains(match, true) || names[it.subjectId].orEmpty().contains(match, true) }.singleOrNull()
    }

    private fun day(text: String): DayOfWeek? {
        val t = text.trim().lowercase()
        return DayOfWeek.entries.firstOrNull { it.name.lowercase() == t || it.name.lowercase().startsWith(t.take(3)) && t.length >= 3 }
    }

    private fun time(text: String): LocalTime? = runCatching { LocalTime.parse(text.trim().let { if (it.length == 4 && it[1] == ':') "0$it" else it }) }.getOrNull()
    private fun date(text: String): LocalDate? = runCatching { LocalDate.parse(text.trim()) }.getOrNull()

    private fun ok(message: String): JsonElement = buildJsonObject { put("ok", true); put("message", message) }
    private fun fail(message: String): JsonElement = buildJsonObject { put("ok", false); put("message", message) }
    private fun missing(name: String): JsonElement = fail("There is no single subject called \"$name\". Add it first or use list_subjects.")
    private fun subjectResult(error: String?, success: String): JsonElement = if (error == null) ok(success) else fail(error)

    private class Prop(val type: String, val description: String)

    private fun text(description: String) = Prop("string", description)
    private fun num(description: String) = Prop("number", description)
    private fun bool(description: String) = Prop("boolean", description)

    private fun schema(props: Map<String, Prop>, required: List<String>) = InputSchema.Obj(
        properties = buildJsonObject {
            props.forEach { (name, p) -> put(name, buildJsonObject { put("type", p.type); put("description", p.description) }) }
        },
        required = required.ifEmpty { null },
    )

    private fun read(name: String, description: String, props: Map<String, Prop>, required: List<String> = emptyList(), run: suspend (JsonObject) -> JsonElement) =
        build(name, description, props, required, ToolApprovalMode.Auto, run)

    private fun write(name: String, description: String, props: Map<String, Prop>, required: List<String> = emptyList(), needsApproval: Boolean = true, run: suspend (JsonObject) -> JsonElement) =
        build(name, description, props, required, if (needsApproval) ToolApprovalMode.RequiresApproval else ToolApprovalMode.Auto, run)

    private fun build(name: String, description: String, props: Map<String, Prop>, required: List<String>, mode: ToolApprovalMode, run: suspend (JsonObject) -> JsonElement) = Tool(
        name = name,
        description = description,
        parameters = { schema(props, required) },
        approvalMode = mode,
        execute = { input ->
            try {
                run(input as? JsonObject ?: JsonObject(emptyMap()))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                fail(e.message ?: "A value is missing.")
            } catch (e: Exception) {
                DebugLogger.e("ChatTools", "Tool $name failed", e)
                fail("That could not be done.")
            }
        },
    )

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    private fun JsonObject.req(key: String): String = str(key) ?: throw IllegalArgumentException("$key is missing.")
    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }
    private fun JsonObject.dbl(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull
    private fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull
}
