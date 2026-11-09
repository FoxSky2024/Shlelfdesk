package com.shelfdeck

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shelfdeck.data.ConsoleDef
import com.shelfdeck.data.Game
import com.shelfdeck.data.LibraryStore
import com.shelfdeck.data.Systems
import com.shelfdeck.engine.EmulationEngine
import com.shelfdeck.engine.LaunchResult
import kotlinx.coroutines.launch

enum class UiMode { SHELF, STEAM }
enum class Overlay { NONE, DETAILS, VIEWER, SETTINGS, BOOT }

val ArtKinds = listOf("front", "back", "disc", "manual")

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val store = LibraryStore(app)
    val engine = EmulationEngine(app)
    private val prefs = app.getSharedPreferences("ui", 0)

    var games by mutableStateOf(store.load()); private set
    var consoleId by mutableStateOf(prefs.getString("console", "all")!!); private set
    var index by mutableIntStateOf(0); private set
    var mode by mutableStateOf(runCatching { UiMode.valueOf(prefs.getString("mode", "SHELF")!!) }.getOrDefault(UiMode.SHELF)); private set
    var overlay by mutableStateOf(Overlay.NONE)
    var viewerPage by mutableIntStateOf(0)
    var steamSelected by mutableStateOf<String?>(null)
    var scanning by mutableStateOf(false); private set
    var toast by mutableStateOf<String?>(null)
    var artVersion by mutableIntStateOf(0); private set
    var bootGame by mutableStateOf<Game?>(null); private set

    private var sessionStart = 0L
    private var sessionGame: String? = null

    // ---- derived ---------------------------------------------------------------------------
    /** Tabs: Home + every console that has games. */
    val tabs: List<ConsoleDef> get() = listOf(Systems.home) + Systems.all.filter { c -> games.any { it.consoleId == c.id } }
    val currentConsole: ConsoleDef get() = Systems.byId(consoleId) ?: Systems.home
    val visible: List<Game>
        get() = games.filter { consoleId == "all" || it.consoleId == consoleId }.sortedBy { it.title.lowercase() }
    val selected: Game? get() = visible.getOrNull(index)
    fun consoleOf(g: Game): ConsoleDef = Systems.byId(g.consoleId) ?: Systems.home

    // ---- navigation ------------------------------------------------------------------------
    fun move(delta: Int) { index = (index + delta).coerceIn(0, (visible.size - 1).coerceAtLeast(0)) }
    fun select(i: Int) { index = i.coerceIn(0, (visible.size - 1).coerceAtLeast(0)) }

    fun selectConsole(id: String) {
        consoleId = id; index = 0
        prefs.edit().putString("console", id).apply()
    }

    fun cycleConsole(delta: Int) {
        val t = tabs
        val i = t.indexOfFirst { it.id == consoleId }.coerceAtLeast(0)
        selectConsole(t[(i + delta + t.size) % t.size].id)
    }

    fun toggleMode() {
        mode = if (mode == UiMode.SHELF) UiMode.STEAM else UiMode.SHELF
        prefs.edit().putString("mode", mode.name).apply()
    }

    fun openViewer(page: Int = 0) { viewerPage = page; overlay = Overlay.VIEWER }
    fun closeOverlay() { overlay = Overlay.NONE }

    // ---- library ---------------------------------------------------------------------------
    fun addRoot(uri: Uri) { store.addRoot(uri); rescan() }
    fun removeRoot(uri: Uri) { store.removeRoot(uri); rescan() }

    fun rescan() = viewModelScope.launch {
        scanning = true
        games = store.scan(store.roots(), games).also { store.save(it) }
        if (tabs.none { it.id == consoleId }) consoleId = "all"
        select(index)
        scanning = false
    }

    private fun update(id: String, f: (Game) -> Game) {
        games = games.map { if (it.id == id) f(it) else it }
        store.save(games)
    }

    fun toggleFavorite(g: Game) = update(g.id) { it.copy(favorite = !it.favorite) }

    fun importArt(g: Game, kind: String, uri: Uri) {
        if (store.importArt(g, kind, uri)) { artVersion++; toast = "Imported $kind art" }
        else toast = "Could not import image"
    }

    // ---- play flow -------------------------------------------------------------------------
    /** Shows the per-console boot screen; it calls [launchBooted] when the animation ends. */
    fun play(g: Game) { bootGame = g; overlay = Overlay.BOOT }

    fun launchBooted(activity: Activity) {
        val g = bootGame ?: return
        overlay = Overlay.NONE
        val console = consoleOf(g)
        when (val r = engine.launch(activity, g, console)) {
            is LaunchResult.Ok -> {
                sessionStart = System.currentTimeMillis(); sessionGame = g.id
                update(g.id) { it.copy(lastPlayed = sessionStart) }
            }
            is LaunchResult.NoEmulatorConfigured -> toast = "No emulator is available for ${console.name} on Android yet"
            is LaunchResult.NotInstalled -> {
                val e = console.emulators.first()
                toast = "Install ${e.label} to play ${console.name}"
                engine.openStorePage(activity, e)
            }
            is LaunchResult.Failed -> toast = r.reason
        }
        bootGame = null
    }

    fun cancelBoot() { bootGame = null; overlay = Overlay.NONE }

    /** Call when returning from the emulator: adds the elapsed time to the game's play time. */
    fun endSession() {
        val id = sessionGame ?: return
        val elapsed = System.currentTimeMillis() - sessionStart
        sessionGame = null
        update(id) { it.copy(playMs = it.playMs + elapsed) }
    }
}
