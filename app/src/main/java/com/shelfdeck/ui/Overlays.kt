package com.shelfdeck.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shelfdeck.ArtKinds
import com.shelfdeck.MainViewModel
import com.shelfdeck.Overlay
import com.shelfdeck.UiMode
import com.shelfdeck.data.Game
import com.shelfdeck.data.Systems
import kotlinx.coroutines.delay

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun rememberArtPicker(vm: MainViewModel): (Game, String) -> Unit {
    var pending by remember { mutableStateOf<Pair<Game, String>?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val p = pending
        if (uri != null && p != null) vm.importArt(p.first, p.second, uri)
    }
    return { g, k -> pending = g to k; launcher.launch(arrayOf("image/*")) }
}

// ============================ Boot screen ("Booting GameCube...") ============================
@Composable
fun BootScreen(vm: MainViewModel) {
    val g = vm.bootGame ?: return
    val c = vm.consoleOf(g)
    val activity = LocalContext.current.findActivity()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(g.id) {
        progress.animateTo(1f, tween(1800, easing = LinearEasing))
        if (activity != null) vm.launchBooted(activity) else vm.cancelBoot()
    }
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(c.bgTop, c.accent.copy(.55f), c.bgBottom))).clickable(enabled = false) {}) {
        Row(Modifier.align(Alignment.Center).padding(40.dp), verticalAlignment = Alignment.CenterVertically) {
            BoxArt(vm, g, "front", Modifier.fillMaxHeight(.62f).aspectRatio(c.boxRatio))
            Spacer(Modifier.width(48.dp))
            Column {
                Text("BOOTING ${c.name.uppercase()}", color = Color.White.copy(.6f), fontSize = 10.sp, letterSpacing = 3.sp)
                Text(c.name, color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Black)
                Text(g.title, color = Color.White, fontSize = 20.sp)
                Text("Starting in ${vm.engine.resolve(c)?.label ?: c.emulators.firstOrNull()?.label ?: "—"}", color = Color.White.copy(.6f), fontSize = 12.sp)
                Spacer(Modifier.height(18.dp))
                LinearProgressIndicator(progress = { progress.value }, color = Color.White, trackColor = Color.White.copy(.2f), modifier = Modifier.width(240.dp))
                Spacer(Modifier.height(10.dp))
                Text("CANCEL", color = Color.White.copy(.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { vm.cancelBoot() }.padding(vertical = 6.dp))
            }
        }
    }
}

// ============================ Details side panel ============================
@Composable
fun DetailsPanel(vm: MainViewModel) {
    val g = (if (vm.mode == UiMode.STEAM) vm.games.firstOrNull { it.id == vm.steamSelected } else vm.selected) ?: return
    val c = vm.consoleOf(g)
    val pickArt = rememberArtPicker(vm)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.45f)).clickable { vm.closeOverlay() }) {
        Column(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(330.dp)
                .background(Color(0xF2101018)).clickable(enabled = false) {}
                .statusBarsPadding().navigationBarsPadding().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(c.name.uppercase(), color = c.accent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text(g.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 3)
            val mins = g.playMs / 60_000
            Text(if (mins == 0L) "Not played yet" else "$mins min played", color = Color.White.copy(.55f), fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            PanelItem("▶  Play", c.accent) { vm.closeOverlay(); vm.play(g) }
            PanelItem(if (g.favorite) "★  Remove favourite" else "☆  Favourite") { vm.toggleFavorite(g) }
            PanelItem("⟲  Inspect box / disc / manual") { vm.openViewer() }
            PanelItem("🖼  Import front cover") { pickArt(g, "front") }
            PanelItem("🖼  Import back cover") { pickArt(g, "back") }
            PanelItem("💿  Import disc art") { pickArt(g, "disc") }
            PanelItem("📖  Import manual page") { pickArt(g, "manual") }
            Spacer(Modifier.weight(1f))
            PanelItem("✕  Close") { vm.closeOverlay() }
        }
    }
}

@Composable
private fun PanelItem(label: String, tint: Color = Color.White, onClick: () -> Unit) =
    Text(label, color = tint, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .background(Color.White.copy(.06f)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp))

// ============================ Inspect viewer: front / back / disc / manual ============================
@Composable
fun ViewerOverlay(vm: MainViewModel) {
    val g = (if (vm.mode == UiMode.STEAM) vm.games.firstOrNull { it.id == vm.steamSelected } else vm.selected) ?: return
    val c = vm.consoleOf(g)
    val rot = remember { Animatable(0f) }
    var shown by remember { mutableIntStateOf(vm.viewerPage) }
    val density = LocalDensity.current.density
    LaunchedEffect(vm.viewerPage) {
        if (shown != vm.viewerPage) {
            rot.animateTo(90f, tween(170)); shown = vm.viewerPage
            rot.snapTo(-90f); rot.animateTo(0f, tween(170))
        }
    }
    val kind = ArtKinds[shown.coerceIn(0, ArtKinds.lastIndex)]
    val label = listOf("COVER", "BACK", "DISC OUT", "MANUAL")[ArtKinds.indexOf(kind)]

    Box(Modifier.fillMaxSize().background(Color.Black.copy(.88f)).clickable { vm.closeOverlay() }) {
        Box(
            Modifier.align(Alignment.Center).fillMaxHeight(.68f)
                .graphicsLayer { rotationY = rot.value; cameraDistance = 16f * density },
        ) {
            if (kind == "disc") {
                BoxArt(vm, g, "disc", Modifier.fillMaxHeight().aspectRatio(1f).clip(CircleShape).border(2.dp, c.accent, CircleShape))
                Box(Modifier.align(Alignment.Center).size(34.dp).clip(CircleShape).background(Color.Black.copy(.85f)))
            } else {
                BoxArt(vm, g, kind, Modifier.fillMaxHeight().aspectRatio(if (kind == "manual") 0.72f else c.boxRatio)
                    .border(1.dp, c.accent.copy(.6f), RoundedCornerShape(4.dp)))
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color.White.copy(.55f), fontSize = 10.sp, letterSpacing = 2.sp)
            Text(g.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            HintBar(listOf("◀ ▶" to "Next side", "A" to "Turn over", "X" to "Import art", "B" to "Close"))
        }
    }
}

// ============================ Settings ============================
@Composable
fun SettingsScreen(vm: MainViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    val cats = listOf("Library", "Emulators", "Display", "Controller")
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> if (uri != null) vm.addRoot(uri) }

    Row(Modifier.fillMaxSize().background(Color(0xFF0E0D16))) {
        Column(Modifier.width(210.dp).fillMaxHeight().background(Color(0xFF12111C)).statusBarsPadding().padding(16.dp)) {
            Text("SETTINGS", color = Color.White.copy(.5f), fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            cats.forEachIndexed { i, n ->
                Text(n, color = if (i == tab) Color.White else Color.White.copy(.6f), fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(if (i == tab) Color.White.copy(.10f) else Color.Transparent)
                        .clickable { tab = i }.padding(12.dp))
            }
            Spacer(Modifier.weight(1f))
            Text("← Back", color = Color(0xFFFF6B6B), modifier = Modifier.clickable { vm.closeOverlay() }.padding(12.dp))
        }
        Column(Modifier.weight(1f).fillMaxHeight().statusBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (tab) {
                0 -> {
                    Text("Game folders", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Put each system in its own folder (e.g. GameCube/, Wii/, PSP/) so shared .iso files are sorted correctly.",
                        color = Color.White.copy(.6f), fontSize = 12.sp)
                    vm.store.roots().forEach { r ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(Uri.decode(r.lastPathSegment ?: r.toString()), color = Color.White.copy(.85f), fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text("Remove", color = Color(0xFFFF6B6B), fontSize = 12.sp, modifier = Modifier.clickable { vm.removeRoot(r) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Pill("＋ Add folder") { folderPicker.launch(null) }
                        Pill(if (vm.scanning) "Scanning…" else "Rescan") { vm.rescan() }
                    }
                    Text("${vm.games.size} games in library", color = Color.White.copy(.6f), fontSize = 12.sp)
                }
                1 -> {
                    Text("Emulators", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Systems.all.forEach { c ->
                        val inst = vm.engine.resolve(c)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(c.name, color = Color.White, fontSize = 13.sp, modifier = Modifier.width(140.dp))
                            Text(
                                inst?.let { "✓ ${it.label}" } ?: c.emulators.joinToString { it.label }.ifEmpty { "No Android emulator" } + "  (not installed)",
                                color = if (inst != null) Color(0xFF6BE675) else Color.White.copy(.5f), fontSize = 12.sp,
                            )
                        }
                    }
                }
                2 -> {
                    Text("Display", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Interface", color = Color.White.copy(.6f), fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Pill(if (vm.mode == UiMode.SHELF) "● 3D Shelf" else "○ 3D Shelf") { if (vm.mode != UiMode.SHELF) vm.toggleMode() }
                        Pill(if (vm.mode == UiMode.STEAM) "● Steam library" else "○ Steam library") { if (vm.mode != UiMode.STEAM) vm.toggleMode() }
                    }
                }
                else -> {
                    Text("Controller", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    listOf(
                        "D-pad / stick ◀ ▶" to "Browse games", "L1 / R1" to "Previous / next system", "A" to "Play (boot screen)",
                        "X" to "Details panel", "Y" to "Inspect box · disc · manual", "Start" to "Settings",
                        "Select" to "Toggle Shelf ⇄ Steam", "B" to "Back / close",
                    ).forEach { (k, v) ->
                        Row { Text(k, color = Color.White, fontSize = 13.sp, modifier = Modifier.width(180.dp)); Text(v, color = Color.White.copy(.65f), fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Pill(label: String, onClick: () -> Unit) =
    Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(50))
        .background(Color.White.copy(.12f)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp))
