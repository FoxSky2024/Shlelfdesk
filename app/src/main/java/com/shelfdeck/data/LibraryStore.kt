package com.shelfdeck.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Game(
    val id: String,
    val title: String,
    val consoleId: String,
    val uri: String,
    val size: Long,
    val lastPlayed: Long = 0,
    val playMs: Long = 0,
    val favorite: Boolean = false,
)

/** Persists the game list (JSON), the scanned root folders, and per-game art files. */
class LibraryStore(private val ctx: Context) {
    private val file = File(ctx.filesDir, "library.json")
    private val prefs = ctx.getSharedPreferences("library", Context.MODE_PRIVATE)

    fun load(): List<Game> = runCatching {
        val arr = JSONArray(file.readText())
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Game(
                o.getString("id"), o.getString("title"), o.getString("console"), o.getString("uri"),
                o.optLong("size"), o.optLong("last"), o.optLong("play"), o.optBoolean("fav"),
            )
        }
    }.getOrDefault(emptyList())

    fun save(games: List<Game>) {
        val arr = JSONArray()
        games.forEach {
            arr.put(JSONObject().put("id", it.id).put("title", it.title).put("console", it.consoleId)
                .put("uri", it.uri).put("size", it.size).put("last", it.lastPlayed)
                .put("play", it.playMs).put("fav", it.favorite))
        }
        file.writeText(arr.toString())
    }

    // ---- scanned folders -------------------------------------------------------------------
    fun roots(): List<Uri> = prefs.getStringSet("roots", emptySet())!!.map(Uri::parse)

    fun addRoot(uri: Uri) {
        runCatching {
            ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        prefs.edit().putStringSet("roots", prefs.getStringSet("roots", emptySet())!! + uri.toString()).apply()
    }

    fun removeRoot(uri: Uri) {
        prefs.edit().putStringSet("roots", prefs.getStringSet("roots", emptySet())!! - uri.toString()).apply()
    }

    /** Walk every root; keep play stats of games that were already known. */
    suspend fun scan(roots: List<Uri>, old: List<Game>): List<Game> = withContext(Dispatchers.IO) {
        val prev = old.associateBy { it.id }
        val out = LinkedHashMap<String, Game>()

        fun walk(dir: DocumentFile, hint: String, depth: Int) {
            for (f in dir.listFiles()) {
                val name = f.name ?: continue
                if (f.isDirectory) {
                    if (depth < 6) walk(f, "$hint/$name", depth + 1)
                    continue
                }
                val ext = name.substringAfterLast('.', "").lowercase()
                val console = Systems.detect(ext, hint) ?: continue
                val id = (console.id + f.uri.toString()).hashCode().toUInt().toString(16)
                val base = prev[id]
                out[id] = Game(
                    id = id,
                    title = cleanTitle(name),
                    consoleId = console.id,
                    uri = f.uri.toString(),
                    size = f.length(),
                    lastPlayed = base?.lastPlayed ?: 0,
                    playMs = base?.playMs ?: 0,
                    favorite = base?.favorite ?: false,
                )
            }
        }

        for (r in roots) {
            val doc = DocumentFile.fromTreeUri(ctx, r) ?: continue
            walk(doc, doc.name ?: "", 0)
        }
        out.values.toList()
    }

    private fun cleanTitle(file: String): String =
        file.substringBeforeLast('.')
            .replace(Regex("""[\[(][^\])]*[\])]"""), "")
            .replace('_', ' ').trim().ifEmpty { file }

    // ---- art -------------------------------------------------------------------------------
    /** kind: front | back | disc | manual */
    fun artFile(g: Game, kind: String): File =
        File(ctx.getExternalFilesDir("art"), "${g.consoleId}/${g.id}_$kind.jpg")

    fun importArt(g: Game, kind: String, src: Uri): Boolean = runCatching {
        val dst = artFile(g, kind).also { it.parentFile?.mkdirs() }
        ctx.contentResolver.openInputStream(src)!!.use { i -> dst.outputStream().use { o -> i.copyTo(o) } }
        true
    }.getOrDefault(false)
}
