package skywa1ker404.absencium.data.trash

import org.junit.Assert.assertEquals
import org.junit.Test

class TrashRetentionTest {
    private val day = 24L * 60 * 60 * 1000

    @Test fun justDeletedHasTheFullRetention() {
        assertEquals(TrashRepository.RETENTION_DAYS, TrashRepository.daysLeft(deletedAt = 1_000_000L, now = 1_000_000L))
    }

    @Test fun countsDownByWholeDays() {
        assertEquals(TrashRepository.RETENTION_DAYS - 1, TrashRepository.daysLeft(0L, day))
        assertEquals(TrashRepository.RETENTION_DAYS - 1, TrashRepository.daysLeft(0L, day * 2 - 1))
        assertEquals(TrashRepository.RETENTION_DAYS - 10, TrashRepository.daysLeft(0L, day * 10))
    }

    @Test fun neverGoesBelowZero() {
        assertEquals(0, TrashRepository.daysLeft(0L, day * TrashRepository.RETENTION_DAYS))
        assertEquals(0, TrashRepository.daysLeft(0L, day * 400))
    }
}
