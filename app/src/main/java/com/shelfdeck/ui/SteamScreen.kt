package com.shelfdeck.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shelfdeck.MainViewModel
import com.shelfdeck.Overlay
import java.text.DateFormat
import java.util.Date

/** Steam-library look: top nav, searchable sidebar grouped by system, hero + green PLAY bar. */
@Composable
fun SteamScreen(vm: MainViewModel) {
    var query by remember { mutableStateOf("") }
    val all = vm.games
    val filtered = remember(all, query) { all.filter { it.title.contains(query, true) }.sortedBy { it.title.lowercase() } }
    val sel = all.firstOrNull { it.id == vm.steamSelected } ?: filtered.firstOrNull()
    LaunchedEffect(sel?.id) { if (vm.steamSelected != sel?.id) vm.steamSelected = sel?.id }

    Column(Modifier.fillMaxSize().background(Steam.bg)) {
        // top nav
        Row(
            Modifier.fillMaxWidth().background(Steam.header).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text("SHELFDECK", color = Steam.text, fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 15.sp)
            listOf("STORE", "LIBRARY", "COMMUNITY").forEach {
                Text(it, color = if (it == "LIBRARY") Color.White else Steam.text.copy(.7f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("Settings", color = Steam.blue, fontSize = 12.sp, modifier = Modifier.clickable { vm.overlay = Overlay.SETTINGS })
            Text("Shelf view", color = Steam.blue, fontSize = 12.sp, modifier = Modifier.clickable { vm.toggleMode() })
        }

        Row(Modifier.fillMaxSize()) {
            // sidebar
            Column(Modifier.width(250.dp).fillMaxHeight().background(Steam.panel)) {
                BasicTextField(
                    query, { query = it }, singleLine = true,
                    textStyle = TextStyle(color = Steam.text, fontSize = 13.sp),
                    cursorBrush = SolidColor(Steam.blue),
                    modifier = Modifier.fillMaxWidth().padding(10.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF2A3A4C)).padding(8.dp),
                    decorationBox = { inner -> if (query.isEmpty()) Text("Search games", color = Steam.text.copy(.45f), fontSize = 13.sp); inner() },
                )
                LazyColumn(Modifier.weight(1f)) {
                    val favs = filtered.filter { it.favorite }
                    if (favs.isNotEmpty()) { item { Section("FAVORITES (${favs.size})") }; items(favs, key = { "f" + it.id }) { GameRow(vm, it, sel?.id) } }
                    vm.tabs.drop(1).forEach { c ->
                        val list = filtered.filter { it.consoleId == c.id }
                        if (list.isNotEmpty()) {
                            item(key = "h" + c.id) { Section("${c.name.uppercase()} (${list.size})") }
                            items(list, key = { it.id }) { GameRow(vm, it, sel?.id) }
                        }
                    }
                }
            }

            // main
            if (sel == null) {
                Box(Modifier.weight(1f).fillMaxHeight(), Alignment.Center) {
                    Text("Add a ROM folder in Settings to fill your library", color = Steam.text)
                }
            } else {
                val c = vm.consoleOf(sel)
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Box(Modifier.fillMaxWidth().height(210.dp).background(Brush.horizontalGradient(listOf(c.accent.copy(.55f), c.bgTop, Steam.bg)))) {
                        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Bottom) {
                            BoxArt(vm, sel, "front", Modifier.height(170.dp).aspectRatio(c.boxRatio))
                            Spacer(Modifier.width(18.dp))
                            Column {
                                Text(c.name.uppercase(), color = Steam.blue, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                                Text(sel.title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                            }
                        }
                    }
                    // action bar
                    Row(Modifier.fillMaxWidth().background(Color.Black.copy(.25f)).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text("▶  PLAY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(Brush.horizontalGradient(listOf(Steam.greenA, Steam.greenB)))
                                .clickable { vm.play(sel) }.padding(horizontal = 36.dp, vertical = 10.dp))
                        Stat("LAST PLAYED", if (sel.lastPlayed == 0L) "Never" else DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(sel.lastPlayed)))
                        Stat("PLAY TIME", hours(sel.playMs))
                        Spacer(Modifier.weight(1f))
                        Text(if (sel.favorite) "★" else "☆", color = Color(0xFFFFD54F), fontSize = 24.sp, modifier = Modifier.clickable { vm.toggleFavorite(sel) })
                        Text("Inspect", color = Steam.blue, fontSize = 12.sp, modifier = Modifier.clickable { vm.steamSelected = sel.id; vm.openViewer() })
                        Text("Details", color = Steam.blue, fontSize = 12.sp, modifier = Modifier.clickable { vm.steamSelected = sel.id; vm.overlay = Overlay.DETAILS })
                    }
                    Column(Modifier.padding(20.dp)) {
                        val emu = vm.engine.resolve(c)
                        Text("EMULATOR", color = Steam.text.copy(.6f), fontSize = 10.sp, letterSpacing = 1.5.sp)
                        Text(emu?.label ?: c.emulators.firstOrNull()?.let { "${it.label} (not installed)" } ?: "None available for Android",
                            color = Steam.text, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable private fun Section(t: String) =
    Text(t, color = Steam.text.copy(.55f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp))

@Composable private fun GameRow(vm: MainViewModel, g: com.shelfdeck.data.Game, selId: String?) =
    Text(g.title, color = if (g.id == selId) Color.White else Steam.text, fontSize = 13.sp, maxLines = 1,
        modifier = Modifier.fillMaxWidth().background(if (g.id == selId) Color(0xFF3D4450) else Color.Transparent)
            .clickable { vm.steamSelected = g.id }.padding(horizontal = 14.dp, vertical = 6.dp))

@Composable private fun Stat(label: String, value: String) = Column {
    Text(label, color = Steam.text.copy(.55f), fontSize = 9.sp, letterSpacing = 1.sp)
    Text(value, color = Steam.text, fontSize = 13.sp)
}

private fun hours(ms: Long): String = when {
    ms < 60_000 -> "—"
    ms < 3_600_000 -> "${ms / 60_000} min"
    else -> "%.1f hrs".format(ms / 3_600_000.0)
}
