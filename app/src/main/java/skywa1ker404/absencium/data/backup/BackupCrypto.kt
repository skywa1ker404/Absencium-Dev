package skywa1ker404.absencium.data.backup

import java.io.ByteArrayOutputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** The password was wrong, or the encrypted file was changed or cut short. The two cannot be told apart, by design. */
class BackupDecryptionException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Password protection for a backup file.
 *
 * No cipher is invented here. The key comes from PBKDF2-HMAC-SHA256 and the data is sealed with AES-256-GCM, both from the
 * platform's own crypto provider. What this class adds is only the framing needed for large files: the data is cut into
 * 64 KiB chunks (GCM on the platform buffers a whole message in memory, which a backup with photos cannot afford), each
 * chunk is sealed on its own, and
 *  - the nonce of a chunk is a random 8-byte prefix plus the chunk's number, so no nonce repeats under one key;
 *  - the header and a "this is the last chunk" flag are authenticated with every chunk, so a changed header, a reordered
 *    chunk or a file cut short at a chunk boundary is detected, not just a flipped bit.
 * The threat model and the known limits are in docs/PRIVACY.md.
 *
 * File layout: `ABSENC01` | version (1 byte) | PBKDF2 iterations (4) | salt (16) | nonce prefix (8) | chunk size (4) | chunks.
 */
object BackupCrypto {

    private val MAGIC = "ABSENC01".toByteArray(Charsets.US_ASCII)
    private const val VERSION = 1
    private const val SALT_BYTES = 16
    private const val PREFIX_BYTES = 8
    private const val TAG_BYTES = 16
    private const val DEFAULT_CHUNK = 64 * 1024
    private const val MAX_CHUNK = 1024 * 1024
    private const val HEADER_BYTES = 8 + 1 + 4 + SALT_BYTES + PREFIX_BYTES + 4

    /** OWASP's 2023 guidance for PBKDF2-HMAC-SHA256. About a second on a phone, once per backup or restore. */
    const val DEFAULT_ITERATIONS = 600_000
    private const val MIN_ITERATIONS = 100_000
    private const val MAX_ITERATIONS = 10_000_000

    /** True when [head] (the first bytes of a file) starts like an encrypted backup. */
    fun looksEncrypted(head: ByteArray): Boolean =
        head.size >= MAGIC.size && MAGIC.indices.all { head[it] == MAGIC[it] }

    const val MAGIC_LENGTH = 8

    fun encrypt(
        out: OutputStream,
        password: CharArray,
        iterations: Int = DEFAULT_ITERATIONS,
        chunkSize: Int = DEFAULT_CHUNK,
        random: SecureRandom = SecureRandom(),
    ): OutputStream {
        require(password.isNotEmpty()) { "The password is empty." }
        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "Unsupported iteration count." }
        require(chunkSize in 1..MAX_CHUNK) { "Unsupported chunk size." }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val prefix = ByteArray(PREFIX_BYTES).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC).put(VERSION.toByte()).putInt(iterations).put(salt).put(prefix).putInt(chunkSize).array()
        out.write(header)
        return Sealer(out, deriveKey(password, salt, iterations), header, prefix, chunkSize)
    }

    /** Reads the header of [input] and returns a stream of the decrypted bytes. Wrong passwords fail on the first read. */
    fun decrypt(input: InputStream, password: CharArray): InputStream {
        val header = ByteArray(HEADER_BYTES)
        if (readFully(input, header, header.size) < header.size) throw BackupDecryptionException("This is not an encrypted Absencium backup.")
        val buffer = ByteBuffer.wrap(header)
        val magic = ByteArray(MAGIC.size).also { buffer.get(it) }
        if (!looksEncrypted(magic)) throw BackupDecryptionException("This is not an encrypted Absencium backup.")
        val version = buffer.get().toInt()
        if (version != VERSION) throw BackupDecryptionException("This backup was encrypted by a newer version of the app.")
        val iterations = buffer.getInt()
        if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) throw BackupDecryptionException("The backup header is damaged.")
        val salt = ByteArray(SALT_BYTES).also { buffer.get(it) }
        val prefix = ByteArray(PREFIX_BYTES).also { buffer.get(it) }
        val chunkSize = buffer.getInt()
        if (chunkSize !in 1..MAX_CHUNK) throw BackupDecryptionException("The backup header is damaged.")
        return Opener(PushbackInputStream(input, 1), deriveKey(password, salt, iterations), header, prefix, chunkSize)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun nonce(prefix: ByteArray, counter: Long): ByteArray {
        if (counter !in 0..0xFFFF_FFFFL) throw IOException("The backup is too large to encrypt.")
        return ByteBuffer.allocate(PREFIX_BYTES + 4).put(prefix).putInt(counter.toInt()).array()
    }

    private fun aad(header: ByteArray, last: Boolean): ByteArray = header + byteArrayOf(if (last) 1 else 0)

    private fun cipher(mode: Int, key: SecretKeySpec, nonce: ByteArray, aad: ByteArray): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, key, GCMParameterSpec(TAG_BYTES * 8, nonce))
            updateAAD(aad)
        }

    private fun readFully(input: InputStream, into: ByteArray, length: Int): Int {
        var read = 0
        while (read < length) {
            val n = input.read(into, read, length - read)
            if (n < 0) break
            read += n
        }
        return read
    }

    private class Sealer(
        out: OutputStream,
        private val key: SecretKeySpec,
        private val header: ByteArray,
        private val prefix: ByteArray,
        private val chunkSize: Int,
    ) : FilterOutputStream(out) {
        private val buffer = ByteArray(chunkSize)
        private var filled = 0
        private var counter = 0L
        private var closed = false

        override fun write(b: Int) {
            write(byteArrayOf(b.toByte()), 0, 1)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            var offset = off
            var left = len
            while (left > 0) {
                // A full buffer is only sealed once more data arrives, because only then is it known not to be the last.
                if (filled == chunkSize) seal(last = false)
                val n = minOf(left, chunkSize - filled)
                System.arraycopy(b, offset, buffer, filled, n)
                filled += n
                offset += n
                left -= n
            }
        }

        private fun seal(last: Boolean) {
            val c = cipher(Cipher.ENCRYPT_MODE, key, nonce(prefix, counter), aad(header, last))
            out.write(c.doFinal(buffer, 0, filled))
            counter++
            filled = 0
        }

        override fun close() {
            if (closed) return
            closed = true
            try {
                seal(last = true)
                out.flush()
            } finally {
                out.close()
            }
        }
    }

    private class Opener(
        private val input: PushbackInputStream,
        private val key: SecretKeySpec,
        private val header: ByteArray,
        private val prefix: ByteArray,
        private val chunkSize: Int,
    ) : InputStream() {
        private var plain = ByteArray(0)
        private var pos = 0
        private var counter = 0L
        private var finished = false

        override fun read(): Int {
            val one = ByteArray(1)
            return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len == 0) return 0
            while (pos >= plain.size) {
                if (finished) return -1
                openNext()
            }
            val n = minOf(len, plain.size - pos)
            System.arraycopy(plain, pos, b, off, n)
            pos += n
            return n
        }

        private fun openNext() {
            val sealed = ByteArray(chunkSize + TAG_BYTES)
            val got = readFully(input, sealed, sealed.size)
            if (got < TAG_BYTES) throw BackupDecryptionException("The backup file is cut short or damaged.")
            val next = input.read()
            val last = next < 0
            if (!last) input.unread(next)
            try {
                val c = cipher(Cipher.DECRYPT_MODE, key, nonce(prefix, counter), aad(header, last))
                plain = c.doFinal(sealed, 0, got)
            } catch (e: GeneralSecurityException) {
                throw BackupDecryptionException("Wrong password, or the backup file was changed or damaged.", e)
            }
            if (!last && got != sealed.size) throw BackupDecryptionException("The backup file is damaged.")
            pos = 0
            counter++
            finished = last
        }

        override fun close() = input.close()
    }

    /** Convenience for small data and tests. */
    fun encryptBytes(data: ByteArray, password: CharArray, iterations: Int = DEFAULT_ITERATIONS, chunkSize: Int = DEFAULT_CHUNK): ByteArray {
        val sink = ByteArrayOutputStream()
        encrypt(sink, password, iterations, chunkSize).use { it.write(data) }
        return sink.toByteArray()
    }

    fun decryptBytes(data: ByteArray, password: CharArray): ByteArray =
        decrypt(data.inputStream(), password).use { it.readBytes() }
}
