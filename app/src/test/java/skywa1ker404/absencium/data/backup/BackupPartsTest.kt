package skywa1ker404.absencium.data.backup

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPartsTest {

    @Test
    fun everyDatabaseTableBelongsToExactlyOnePart() {
        val entities = File("src/main/java/skywa1ker404/absencium/data/db/Entities.kt").readText()
        val tables = Regex("tableName\\s*=\\s*\"([A-Za-z_]+)\"").findAll(entities).map { it.groupValues[1] }.toSet()
        assertTrue("no tables found, the test path is wrong", tables.size >= 19)
        assertEquals("tables without a part", emptySet<String>(), tables - BackupPart.allTables)
        assertEquals("parts naming a table that does not exist", emptySet<String>(), BackupPart.allTables - tables)
        val listed = BackupPart.entries.flatMap { it.tables }
        assertEquals("a table is in two parts", listed.size, listed.toSet().size)
    }

    @Test
    fun needsOnlyPointToEarlierParts() {
        BackupPart.entries.forEachIndexed { index, part ->
            part.needs.forEach { assertTrue("${part.name} needs ${it.name}", it.ordinal < index) }
        }
    }

    @Test
    fun switchingAPartOnSwitchesItsNeedsOn() {
        val result = BackupPart.withNeeds(setOf(BackupPart.NOTE_FILES))
        assertEquals(setOf(BackupPart.NOTE_FILES, BackupPart.NOTES, BackupPart.SUBJECTS), result)
    }

    @Test
    fun switchingARootOffSwitchesEverythingThatNeedsItOff() {
        val all = BackupPart.entries.toSet()
        val result = BackupPart.without(all, BackupPart.NOTES)
        assertFalse(BackupPart.NOTES in result)
        assertFalse(BackupPart.NOTE_FILES in result)
        assertFalse(BackupPart.NOTE_HISTORY in result)
        assertFalse(BackupPart.RECORDINGS in result)
        assertFalse(BackupPart.RECALL in result)
        assertFalse(BackupPart.AI_HISTORY in result)
        assertTrue(BackupPart.SUBJECTS in result)
        assertTrue(BackupPart.DOCUMENTS in result)
        assertTrue(BackupPart.EXAMS in result)
    }

    @Test
    fun switchingALeafOffKeepsItsNeeds() {
        val result = BackupPart.without(BackupPart.entries.toSet(), BackupPart.NOTE_FILES)
        assertTrue(BackupPart.NOTES in result)
        assertTrue(BackupPart.SUBJECTS in result)
    }

    @Test
    fun dependentsAreTransitive() {
        val dependents = BackupPart.dependentsOf(BackupPart.SUBJECTS)
        assertTrue(BackupPart.NOTES in dependents)
        assertTrue(BackupPart.NOTE_FILES in dependents)
        assertFalse(BackupPart.STATS in dependents)
    }

    @Test
    fun presetsAreClosedUnderNeeds() {
        listOf(BackupPart.everything, BackupPart.lightWithoutMedia, BackupPart.dataOnly).forEach {
            assertEquals(it, BackupPart.withNeeds(it))
        }
        assertFalse(BackupPart.NOTE_FILES in BackupPart.lightWithoutMedia)
        assertTrue(BackupPart.NOTES in BackupPart.dataOnly)
    }

    @Test
    fun savedChoiceRoundTripsAndIgnoresUnknownNames() {
        val choice = BackupPart.withNeeds(setOf(BackupPart.RECORDINGS, BackupPart.STATS))
        assertEquals(choice, BackupPart.parse(BackupPart.format(choice)))
        assertEquals(setOf(BackupPart.SUBJECTS), BackupPart.parse("SUBJECTS,FROM_A_NEWER_APP"))
        assertEquals(null, BackupPart.parse(null))
    }

    @Test
    fun manifestPartsSurviveJson() {
        val m = BackupManifest(2, "1", 5, 1L, true, false, emptyMap(), emptyList(), listOf(ManifestPart("NOTES", 3, 0, 120L)))
        assertEquals(m.parts, BackupManifestJson.read(BackupManifestJson.write(m)).parts)
        val old = BackupManifest(1, "1", 3, 1L, true, false, emptyMap(), emptyList())
        assertEquals(emptyList<ManifestPart>(), BackupManifestJson.read(BackupManifestJson.write(old)).parts)
    }

    @Test
    fun everyLinkColumnIsKnownToTheMerge() {
        val entities = File("src/main/java/skywa1ker404/absencium/data/db/Entities.kt").readText()
        val links = Regex("name\\s*=\\s*\"([a-z_]+_id)\"").findAll(entities).map { it.groupValues[1] }.toSet()
        assertEquals(emptySet<String>(), links - setOf("subject_id", "note_id", "parent_id", "syllabus_node_id", "question_id"))
    }
}
