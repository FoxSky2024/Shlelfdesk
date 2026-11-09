package com.shelfdeck.data

import androidx.compose.ui.graphics.Color

/**
 * An emulator app that ShelfDeck can hand a game to.
 * [activity] is optional; when null the OS resolves ACTION_VIEW inside [pkg].
 * Package / activity names are best-effort and may differ between forks and versions.
 */
data class EmulatorDef(val label: String, val pkg: String, val activity: String? = null)

data class ConsoleDef(
    val id: String,
    val name: String,
    val maker: String,
    val extensions: Set<String>,
    val accent: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val emulators: List<EmulatorDef>,
    val boxRatio: Float = 0.74f,
    /** Normalised (lowercase, a-z0-9) folder-name fragments used to disambiguate shared extensions like .iso */
    val folderHints: List<String> = listOf(id),
)

object Systems {
    val home = ConsoleDef(
        "all", "Home", "ShelfDeck", emptySet(),
        Color(0xFF8E7CFF), Color(0xFF1B1745), Color(0xFF07060F), emptyList(),
    )

    private val dolphin = EmulatorDef("Dolphin", "org.dolphinemu.dolphinemu", "org.dolphinemu.dolphinemu.ui.main.MainActivity")
    private val ppsspp = EmulatorDef("PPSSPP", "org.ppsspp.ppsspp", "org.ppsspp.ppsspp.PpssppActivity")
    private val ppssppGold = EmulatorDef("PPSSPP Gold", "org.ppsspp.ppssppgold", "org.ppsspp.ppssppgold.PpssppActivity")

    val all: List<ConsoleDef> = listOf(
        ConsoleDef("gamecube", "GameCube", "Nintendo", setOf("iso", "gcm", "rvz", "gcz", "ciso"),
            Color(0xFF8C6CFF), Color(0xFF2A2170), Color(0xFF080616), listOf(dolphin), 0.80f, listOf("gamecube", "gc", "ngc")),
        ConsoleDef("wii", "Wii", "Nintendo", setOf("iso", "wbfs", "rvz", "wad"),
            Color(0xFF5BC8F5), Color(0xFF2B4A63), Color(0xFF0A1118), listOf(dolphin), 0.74f, listOf("wii")),
        ConsoleDef("wiiu", "Wii U", "Nintendo", setOf("wud", "wux", "wua", "rpx"),
            Color(0xFF29B6F6), Color(0xFF4A6075), Color(0xFF0C141B), emptyList(), 0.74f, listOf("wiiu")),
        ConsoleDef("switch", "Switch", "Nintendo", setOf("nsp", "xci", "nca"),
            Color(0xFFFF4D5A), Color(0xFF3A1117), Color(0xFF0D0507),
            listOf(EmulatorDef("Eden", "dev.eden.eden_emulator")), 0.62f, listOf("switch", "nx")),
        ConsoleDef("psp", "PSP", "Sony", setOf("iso", "cso", "pbp"),
            Color(0xFF6C7BFF), Color(0xFF1E1B5E), Color(0xFF05041A), listOf(ppsspp, ppssppGold), 0.57f, listOf("psp")),
        ConsoleDef("ps2", "PlayStation 2", "Sony", setOf("iso", "chd", "cso"),
            Color(0xFF3D6BFF), Color(0xFF111C52), Color(0xFF04060F),
            listOf(EmulatorDef("NetherSX2", "xyz.aethersx2.android")), 0.72f, listOf("ps2", "playstation2")),
        ConsoleDef("xbox360", "Xbox 360", "Microsoft", setOf("xex", "god"),
            Color(0xFF5CFF4A), Color(0xFF14661C), Color(0xFF030A04), emptyList(), 0.72f, listOf("xbox360", "x360")),
        ConsoleDef("3ds", "3DS", "Nintendo", setOf("3ds", "cci", "cxi", "cia"),
            Color(0xFFFF5A5A), Color(0xFF47202A), Color(0xFF0C0608),
            listOf(EmulatorDef("Azahar", "org.azahar_emu.azahar")), 0.88f, listOf("3ds", "n3ds")),
        ConsoleDef("nds", "Nintendo DS", "Nintendo", setOf("nds"),
            Color(0xFFB0B8C4), Color(0xFF30343C), Color(0xFF0A0B0D),
            listOf(EmulatorDef("melonDS", "me.magnum.melonds")), 0.88f, listOf("nds", "ds")),
        ConsoleDef("vita", "PS Vita", "Sony", setOf("vpk"),
            Color(0xFF4FA3FF), Color(0xFF14284A), Color(0xFF05080F),
            listOf(EmulatorDef("Vita3K", "org.vita3k.emulator")), 0.78f, listOf("vita", "psvita")),
    )

    fun byId(id: String): ConsoleDef? = all.firstOrNull { it.id == id }

    /** Pick the console for a file: extension candidates, disambiguated by the folder path. */
    fun detect(ext: String, pathHint: String): ConsoleDef? {
        val cands = all.filter { ext in it.extensions }
        if (cands.size <= 1) return cands.firstOrNull()
        val hint = pathHint.lowercase().filter { it.isLetterOrDigit() }
        // longest matching hint wins so that "wiiu" beats "wii"
        return cands.mapNotNull { c ->
            c.folderHints.filter { it in hint }.maxOfOrNull { it.length }?.let { c to it }
        }.maxByOrNull { it.second }?.first
    }
}
