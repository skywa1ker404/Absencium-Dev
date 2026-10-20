package skywa1ker404.absencium.data.backup

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Reads and writes [BackupManifest] as JSON. Kept apart from the rules so those can be tested without Android's org.json. */
object BackupManifestJson {

    fun write(manifest: BackupManifest): ByteArray {
        val counts = JSONObject()
        manifest.counts.toSortedMap().forEach { (k, v) -> counts.put(k, v) }
        val files = JSONArray()
        manifest.files.forEach {
            files.put(JSONObject().put("path", it.path).put("size", it.size).put("sha256", it.sha256))
        }
        val json = JSONObject()
            .put("format", "absencium-backup")
            .put("formatVersion", manifest.formatVersion)
            .put("appVersion", manifest.appVersion)
            .put("schemaVersion", manifest.schemaVersion)
            .put("createdAt", manifest.createdAt)
            .put("includesAttachments", manifest.includesAttachments)
            .put("encrypted", manifest.encrypted)
            .put("counts", counts)
            .put("files", files)
        return json.toString(2).toByteArray(Charsets.UTF_8)
    }

    /** Throws [IllegalArgumentException] with a readable reason when the bytes are not a manifest. */
    fun read(bytes: ByteArray): BackupManifest {
        try {
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            require(json.optString("format") == "absencium-backup") { "This is not an Absencium backup manifest." }
            val countsJson = json.optJSONObject("counts") ?: JSONObject()
            val counts = HashMap<String, Int>()
            val keys = countsJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                counts[key] = countsJson.optInt(key)
            }
            val filesJson = json.getJSONArray("files")
            val files = (0 until filesJson.length()).map {
                val f = filesJson.getJSONObject(it)
                ManifestFile(f.getString("path"), f.getLong("size"), f.getString("sha256"))
            }
            return BackupManifest(
                formatVersion = json.getInt("formatVersion"),
                appVersion = json.optString("appVersion"),
                schemaVersion = json.getInt("schemaVersion"),
                createdAt = json.optLong("createdAt"),
                includesAttachments = json.optBoolean("includesAttachments"),
                encrypted = json.optBoolean("encrypted"),
                counts = counts,
                files = files,
            )
        } catch (e: JSONException) {
            throw IllegalArgumentException("The manifest is damaged: ${e.message}")
        }
    }
}
