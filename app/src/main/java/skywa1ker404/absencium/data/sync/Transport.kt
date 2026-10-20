package skywa1ker404.absencium.data.sync

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Where a sealed blob (a backup package or, later, an encrypted change set) is stored. Nothing above this interface knows
 * whether the other end is a folder on the phone, a WebDAV server or anything else, so sync can be added later without
 * touching backup or restore. Names are flat (no slashes); implementations refuse anything else.
 *
 * The only implementation today is [LocalFileTransport]. No transport reaches the network, and a network one must be
 * switched on by the user before it is ever created.
 */
interface Transport {
    /** Names of the blobs stored, oldest first. */
    fun list(): List<String>

    /** Writes [name] atomically: a reader never sees half a blob, and a failed write leaves the old one intact. */
    fun put(name: String, write: (OutputStream) -> Unit)

    /** Opens [name] for reading, or returns null when it does not exist. */
    fun open(name: String): InputStream?

    fun delete(name: String): Boolean
}

class LocalFileTransport(private val directory: File) : Transport {

    private fun fileFor(name: String): File {
        require(name.isNotEmpty() && name.length <= 200 && name.none { it == '/' || it == '\\' || it == '\u0000' } && name != "." && name != "..") {
            "Not a valid blob name."
        }
        return File(directory, name)
    }

    override fun list(): List<String> =
        directory.listFiles { f -> f.isFile && !f.name.endsWith(PARTIAL) }.orEmpty().sortedBy { it.lastModified() }.map { it.name }

    override fun put(name: String, write: (OutputStream) -> Unit) {
        val target = fileFor(name)
        if (!directory.exists() && !directory.mkdirs()) throw IOException("Could not create the folder.")
        val partial = File(directory, name + PARTIAL)
        try {
            partial.outputStream().use { write(it) }
            if (!partial.renameTo(target)) throw IOException("Could not move the finished file into place.")
        } finally {
            if (partial.exists()) partial.delete()
        }
    }

    override fun open(name: String): InputStream? = fileFor(name).takeIf { it.isFile }?.inputStream()

    override fun delete(name: String): Boolean = fileFor(name).delete()

    private companion object {
        const val PARTIAL = ".partial"
    }
}
