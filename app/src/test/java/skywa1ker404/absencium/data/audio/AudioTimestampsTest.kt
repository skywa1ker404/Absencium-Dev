package skywa1ker404.absencium.data.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioTimestampsTest {
    @Test fun roundTripSortsAndDropsNegatives() {
        assertEquals("[100,2000,9000]", AudioTimestamps.encode(listOf(9000, -5, 100, 2000)))
        assertEquals(listOf(100L, 2000L, 9000L), AudioTimestamps.decode("[100,2000,9000]"))
        assertEquals(emptyList<Long>(), AudioTimestamps.decode(AudioTimestamps.encode(emptyList())))
    }

    @Test fun badInputGivesEmptyList() {
        assertEquals(emptyList<Long>(), AudioTimestamps.decode(""))
        assertEquals(emptyList<Long>(), AudioTimestamps.decode("[1,x]"))
        assertEquals(emptyList<Long>(), AudioTimestamps.decode("[-1]"))
        assertEquals(emptyList<Long>(), AudioTimestamps.decode("not json"))
    }

    @Test fun timesAreFormatted() {
        assertEquals("0:00", AudioTimestamps.format(0))
        assertEquals("0:05", AudioTimestamps.format(5_999))
        assertEquals("12:34", AudioTimestamps.format((12 * 60 + 34) * 1000L))
        assertEquals("1:02:03", AudioTimestamps.format((3600 + 2 * 60 + 3) * 1000L))
        assertEquals("0:00", AudioTimestamps.format(-10))
    }
}
