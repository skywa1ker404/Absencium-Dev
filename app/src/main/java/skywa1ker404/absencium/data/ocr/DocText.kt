package skywa1ker404.absencium.data.ocr

import java.io.File
import java.util.zip.ZipFile

/** Plain text out of office files, without any network or AI. Pure apart from reading the file. */
object DocText {

    private val paragraphEnd = Regex("</w:p>|<w:br[^>]*/>|<w:cr[^>]*/>")
    private val tab = Regex("<w:tab[^>]*/>")
    private val anyTag = Regex("<[^>]+>")
    private val manyBlankLines = Regex("\n{3,}")

    /** The text of a Word document's `word/document.xml`: one line per paragraph. */
    fun fromDocxXml(xml: String): String {
        val withBreaks = xml.replace(paragraphEnd, "\n").replace(tab, "\t")
        val stripped = anyTag.replace(withBreaks, "")
        return decodeEntities(stripped).replace(manyBlankLines, "\n\n").trim()
    }

    private fun decodeEntities(text: String): String =
        text.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")

    /** The text of a .docx file, or null when it is not a readable Word document. */
    fun readDocx(file: File): String? = try {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("word/document.xml") ?: return null
            fromDocxXml(zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText())
        }
    } catch (e: Exception) {
        null
    }

    /** The text of a plain text or Markdown file, capped so a huge file cannot fill the memory. */
    fun readPlain(file: File, maxBytes: Long = 2_000_000L): String? = try {
        if (file.length() > maxBytes) null else file.readText(Charsets.UTF_8).trim()
    } catch (e: Exception) {
        null
    }
}
