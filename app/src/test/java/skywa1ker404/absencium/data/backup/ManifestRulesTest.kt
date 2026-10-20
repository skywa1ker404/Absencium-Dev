package skywa1ker404.absencium.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestRulesTest {

    private val db = "db-bytes".toByteArray()
    private val photo = "photo-bytes".toByteArray()

    private fun entry(path: String, bytes: ByteArray) = ManifestFile(path, bytes.size.toLong(), ManifestRules.sha256Hex(bytes))

    private fun manifest(
        files: List<ManifestFile> = listOf(entry(ManifestRules.DATABASE, db), entry("attachments/a.jpg", photo)),
        formatVersion: Int = ManifestRules.FORMAT_VERSION,
        schemaVersion: Int = 2,
    ) = BackupManifest(formatVersion, "1.0.0", schemaVersion, 0L, true, false, emptyMap(), files)

    private val actual = mapOf(
        ManifestRules.DATABASE to (db.size.toLong() to ManifestRules.sha256Hex(db)),
        "attachments/a.jpg" to (photo.size.toLong() to ManifestRules.sha256Hex(photo)),
        ManifestRules.MANIFEST to (1L to "ignored"),
    )

    @Test fun intactPackagePasses() {
        assertEquals(emptyList<String>(), ManifestRules.problems(manifest(), actual, currentSchemaVersion = 2))
    }

    @Test fun olderSchemaIsAccepted() {
        assertTrue(ManifestRules.problems(manifest(schemaVersion = 1), actual, 2).isEmpty())
    }

    @Test fun newerSchemaIsRefused() {
        assertFalse(ManifestRules.problems(manifest(schemaVersion = 3), actual, 2).isEmpty())
    }

    @Test fun newerFormatIsRefused() {
        assertFalse(ManifestRules.problems(manifest(formatVersion = 2), actual, 2).isEmpty())
    }

    @Test fun changedFileFailsItsChecksum() {
        val tampered = actual + (ManifestRules.DATABASE to (db.size.toLong() to ManifestRules.sha256Hex("other".toByteArray())))
        assertFalse(ManifestRules.problems(manifest(), tampered, 2).isEmpty())
    }

    @Test fun wrongSizeIsReported() {
        val tampered = actual + ("attachments/a.jpg" to (999L to ManifestRules.sha256Hex(photo)))
        assertFalse(ManifestRules.problems(manifest(), tampered, 2).isEmpty())
    }

    @Test fun missingListedFileIsReported() {
        assertFalse(ManifestRules.problems(manifest(), actual - "attachments/a.jpg", 2).isEmpty())
    }

    @Test fun unlistedExtraFileIsReported() {
        val extra = actual + ("attachments/sneaky.bin" to (1L to "00"))
        assertFalse(ManifestRules.problems(manifest(), extra, 2).isEmpty())
    }

    @Test fun manifestWithoutDatabaseIsRefused() {
        val noDb = manifest(files = listOf(entry("attachments/a.jpg", photo)))
        assertFalse(ManifestRules.problems(noDb, actual - ManifestRules.DATABASE, 2).isEmpty())
    }

    @Test fun duplicateEntriesAreReported() {
        val twice = manifest(files = listOf(entry(ManifestRules.DATABASE, db), entry(ManifestRules.DATABASE, db)))
        assertFalse(ManifestRules.problems(twice, actual, 2).isEmpty())
    }

    @Test fun unsafeEntryNamesAreRefused() {
        val bad = listOf(
            "../Absencium.db", "attachments/../x", "attachments/..", "attachments/.", "attachments/", "attachments/a/b.jpg",
            "attachments\\a.jpg", "/etc/passwd", "database/other.db", "attachments/a\u0000.jpg", "", "manifest.json/",
        )
        for (name in bad) assertFalse("should refuse '$name'", ManifestRules.isSafeEntryName(name))
        for (name in listOf("manifest.json", "settings.json", "database/Absencium.db", "attachments/a1b2.jpg")) {
            assertTrue("should accept '$name'", ManifestRules.isSafeEntryName(name))
        }
    }

    @Test fun manifestNamingAnUnsafePathIsReported() {
        val m = manifest(files = listOf(entry(ManifestRules.DATABASE, db), entry("../evil", photo)))
        assertFalse(ManifestRules.problems(m, actual, 2).isEmpty())
    }

    @Test fun sha256MatchesKnownVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", ManifestRules.sha256Hex("abc".toByteArray()))
        assertEquals(
            ManifestRules.sha256Hex("abc".toByteArray()),
            ManifestRules.sha256Hex("abc".byteInputStream()),
        )
    }
}
