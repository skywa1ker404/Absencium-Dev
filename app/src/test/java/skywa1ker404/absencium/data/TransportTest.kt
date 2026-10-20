package skywa1ker404.absencium.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import skywa1ker404.absencium.data.sync.LocalFileTransport
import skywa1ker404.absencium.data.sync.ReadOnlyScope

class TransportTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun putOpenListDelete() {
        val t = LocalFileTransport(tmp.newFolder("blobs"))
        assertTrue(t.list().isEmpty())
        t.put("a.bin") { it.write(byteArrayOf(1, 2, 3)) }
        assertEquals(listOf("a.bin"), t.list())
        assertArrayEquals(byteArrayOf(1, 2, 3), t.open("a.bin")!!.use { it.readBytes() })
        assertNull(t.open("missing"))
        assertTrue(t.delete("a.bin"))
        assertFalse(t.delete("a.bin"))
    }

    @Test fun failedWriteKeepsTheOldBlobAndLeavesNoPartialFile() {
        val dir = tmp.newFolder("blobs2")
        val t = LocalFileTransport(dir)
        t.put("a.bin") { it.write(byteArrayOf(9)) }
        try {
            t.put("a.bin") { it.write(byteArrayOf(1)); throw IllegalStateException("boom") }
            fail("expected the failure to propagate")
        } catch (_: IllegalStateException) {
        }
        assertArrayEquals(byteArrayOf(9), t.open("a.bin")!!.use { it.readBytes() })
        assertEquals(listOf("a.bin"), dir.list()!!.toList())
    }

    @Test fun pathsInNamesAreRefused() {
        val t = LocalFileTransport(tmp.newFolder("blobs3"))
        for (name in listOf("../x", "a/b", "a\\b", "..", ".", "")) {
            try {
                t.put(name) { }
                fail("should refuse '$name'")
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    @Test fun emptyScopeSharesNothing() {
        val scope = ReadOnlyScope()
        assertTrue(scope.isEmpty)
        assertFalse(scope.allows("any"))
        assertTrue(ReadOnlyScope(subjectUuids = setOf("u1")).allows("u1"))
    }
}
