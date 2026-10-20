package skywa1ker404.absencium.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Random

class BackupCryptoTest {

    // The smallest iteration count the format accepts keeps the tests fast; the default is only a constant.
    private val iterations = 100_000
    private val password = "correct horse".toCharArray()

    private fun sample(size: Int): ByteArray = ByteArray(size).also { Random(42).nextBytes(it) }

    private fun seal(data: ByteArray, chunk: Int = 1024, pw: CharArray = password) =
        BackupCrypto.encryptBytes(data, pw, iterations, chunk)

    private fun assertRejected(block: () -> Unit) {
        try {
            block()
            fail("Expected BackupDecryptionException")
        } catch (_: BackupDecryptionException) {
            // expected
        }
    }

    @Test fun roundTripsVariousSizes() {
        // Empty, one byte, just under / exactly at / just over a chunk, and several chunks.
        for (size in listOf(0, 1, 1023, 1024, 1025, 2048, 5000)) {
            val data = sample(size)
            assertArrayEquals("size $size", data, BackupCrypto.decryptBytes(seal(data), password))
        }
    }

    @Test fun outputLooksEncryptedAndHidesThePlaintext() {
        val data = "SQLite format 3\u0000 and some more recognisable text".toByteArray()
        val sealed = seal(data)
        assertTrue(BackupCrypto.looksEncrypted(sealed))
        assertFalse(String(sealed, Charsets.ISO_8859_1).contains("recognisable"))
        assertFalse(BackupCrypto.looksEncrypted(data))
    }

    @Test fun sameInputGivesDifferentFiles() {
        val data = sample(100)
        assertFalse(seal(data).contentEquals(seal(data)))
    }

    @Test fun wrongPasswordIsRejected() {
        val sealed = seal(sample(3000))
        assertRejected { BackupCrypto.decryptBytes(sealed, "wrong".toCharArray()) }
    }

    @Test fun flippedBitInEveryRegionIsRejected() {
        val sealed = seal(sample(3000))
        // Header fields, then the body of the first chunk, then the very last byte (the final tag).
        for (index in listOf(10, 14, 20, 40, 60, 100, sealed.size / 2, sealed.size - 1)) {
            val copy = sealed.copyOf()
            copy[index] = (copy[index].toInt() xor 0x01).toByte()
            assertRejected { BackupCrypto.decryptBytes(copy, password) }
        }
    }

    @Test fun truncationIsRejectedEvenAtAChunkBoundary() {
        val chunk = 1024
        val sealed = seal(sample(chunk * 3 + 10), chunk)
        val header = sealed.size - ((chunk + 16) * 3 + 10 + 16)
        // Cut after the first and after the second whole chunk, and in the middle of the last.
        for (cut in listOf(header + chunk + 16, header + 2 * (chunk + 16), sealed.size - 5, header + 3)) {
            assertRejected { BackupCrypto.decryptBytes(sealed.copyOf(cut), password) }
        }
    }

    @Test fun reorderedChunksAreRejected() {
        val chunk = 1024
        val sealed = seal(sample(chunk * 3), chunk)
        val header = sealed.size - 3 * (chunk + 16) // exactly three full chunks; the last is sealed on close
        val size = chunk + 16
        val swapped = sealed.copyOf()
        System.arraycopy(sealed, header + size, swapped, header, size)
        System.arraycopy(sealed, header, swapped, header + size, size)
        assertRejected { BackupCrypto.decryptBytes(swapped, password) }
    }

    @Test fun notAnEncryptedFileIsRejected() {
        assertRejected { BackupCrypto.decryptBytes(ByteArray(10), password) }
        assertRejected { BackupCrypto.decryptBytes(sample(200), password) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyPasswordIsRefused() {
        BackupCrypto.encryptBytes(ByteArray(1), CharArray(0), iterations)
    }

    @Test fun absurdIterationCountInHeaderIsRejected() {
        val sealed = seal(sample(10))
        // Bytes 9..12 hold the iteration count; make it huge so a crafted file cannot stall the phone.
        sealed[9] = 0x7F
        assertRejected { BackupCrypto.decryptBytes(sealed, password) }
    }
}
