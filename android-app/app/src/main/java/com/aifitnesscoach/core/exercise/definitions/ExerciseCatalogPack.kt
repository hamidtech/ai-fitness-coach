package com.aifitnesscoach.core.exercise.definitions

import android.graphics.BitmapFactory
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

internal const val MAX_PACK_BYTES = 8 * 1024 * 1024

internal data class ExerciseCatalogPack(val version: Int, val data: JSONObject, val atlas: ByteArray) {
    companion object {
        fun read(input: InputStream): ExerciseCatalogPack {
            val limits = mapOf("manifest.json" to 1024, "catalog.json" to 8 * 1024 * 1024,
                "thumbnails.webp" to 3 * 1024 * 1024)
            val files = mutableMapOf<String, ByteArray>()
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val limit = requireNotNull(limits[entry.name])
                    require(!entry.isDirectory && entry.name !in files)
                    files[entry.name] = zip.readBounded(limit)
                }
            }
            val manifest = JSONObject(files.getValue("manifest.json").decodeToString())
            require(manifest.getInt("schema") == 1)
            val version = manifest.getInt("version")
            require(version > 0)
            return ExerciseCatalogPack(version, JSONObject(files.getValue("catalog.json").decodeToString()),
                files.getValue("thumbnails.webp"))
        }
    }

    fun tileCount(): Int {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(atlas, 0, atlas.size, options)
        val edge = ExternalExerciseSources.TILE_EDGE
        require(options.outMimeType == "image/webp" &&
            options.outWidth == ExternalExerciseSources.ATLAS_COLUMNS * edge &&
            options.outHeight in edge..(edge * 32) && options.outHeight % edge == 0)
        return (options.outHeight / edge) * ExternalExerciseSources.ATLAS_COLUMNS
    }
}

internal fun InputStream.readBounded(limit: Int): ByteArray {
    val bytes = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count == -1) return bytes.toByteArray()
        require(count <= limit - bytes.size()) { "Catalog file exceeds size limit" }
        bytes.write(buffer, 0, count)
    }
}

internal fun pinnedPublicMediaUrl(value: String): String {
    val url = requireNotNull(value.toHttpUrlOrNull())
    require(url.isHttps && url.host == "raw.githubusercontent.com" && url.port == 443 &&
        url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null &&
        url.pathSegments.size >= 4 && url.pathSegments[2].matches(Regex("[0-9a-f]{40}")))
    return url.toString()
}
