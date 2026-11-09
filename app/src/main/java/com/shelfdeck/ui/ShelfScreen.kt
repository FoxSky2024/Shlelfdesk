package com.shelfdeck.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.shelfdeck.MainViewModel
import com.shelfdeck.Overlay
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs

@Composable
fun ShelfScreen(vm: MainViewModel) {
    val console = vm.currentConsole
    val games = vm.visible
    val pos by animateFloatAsState(vm.index.toFloat(), spring(stiffness = Spring.StiffnessMediumLow), label = "shelfPos")
    val top by animateColorAsState2(console.bgTop)
    val density = LocalDensity.current.density

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, console.bgBottom)))) {

        // ---- 3D shelf ----------------------------------------------------------------------
        BoxWithConstraints(
            Modifier.fillMaxSize().pointerInput(games.size) {
                var acc = 0f
                detectHorizontalDragGestures(onDragStart = { acc = 0f }) { _, dx ->
                    acc += dx
                    if (acc > 110f) { vm.move(-1); acc = 0f } else if (acc < -110f) { vm.move(1); acc = 0f }
                }
            },
        ) {
            val cardH = maxHeight * 0.50f
            val cardW = cardH * console.boxRatio
            val cardWpx = with(LocalDensity.current) { cardW.toPx() }

            if (games.isEmpty()) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No games yet", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Open Settings ▸ Library and add your ROM folder", color = Color.White.copy(.7f))
                    Spacer(Modifier.height(16.dp))
                    Text("OPEN SETTINGS", color = console.accent, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { vm.overlay = Overlay.SETTINGS }.padding(12.dp))
                }
            }

            for (i in (vm.index - 7)..(vm.index + 7)) {
                val g = games.getOrNull(i) ?: continue
                key(g.id) {
                    val d = i - pos
                    val near = d.coerceIn(-1f, 1f)
                    val far = d - near
                    val scale = if (abs(d) < 1f) 1.22f - 0.30f * abs(d) else 0.92f
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .offset(y = -cardH * 0.04f)
                            .size(cardW, cardH)
                            .zIndex(-abs(d))
                            .graphicsLayer {
                                translationX = near * cardWpx * 1.25f + far * cardWpx * 0.36f
                                rotationY = -near * 58f
                                scaleX = scale; scaleY = scale
                                cameraDistance = 14f * density
                                alpha = (1f - (abs(d) - 4f).coerceAtLeast(0f) * 0.3f).coerceIn(0f, 1f)
                            }
                            .clickable { if (i == vm.index) vm.overlay = Overlay.DETAILS else vm.select(i) },
                    ) {
                        BoxArt(vm, g, "front", Modifier.fillMaxSize())
                        // reflection on the "shelf"
                        Box(
                            Modifier
                                .offset(y = cardH)
                                .size(cardW, cardH)
                                .graphicsLayer { scaleY = -1f; alpha = 0.28f; compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black),
                                        blendMode = BlendMode.DstIn,
                                    )
                                },
                        ) { BoxArt(vm, g, "front", Modifier.fillMaxSize()) }
                    }
                }
            }
        }

        // ---- header ------------------------------------------------------------------------
        Column(Modifier.align(Alignment.TopStart).fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(190.dp)) {
                    Text(console.name.uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    Text("${console.maker} · ${games.size} games".uppercase(), color = Color.White.copy(.55f), fontSize = 9.sp, letterSpacing = 1.5.sp)
                }
                LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(vm.tabs) { _, c ->
                        val sel = c.id == vm.currentConsole.id
                        Text(
                            c.name,
                            color = if (sel) Color.Black else Color.White.copy(.75f),
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(50))
                                .background(if (sel) Color.White else Color.White.copy(.10f))
                                .clickable { vm.selectConsole(c.id) }
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date()), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (games.isNotEmpty()) Text("${vm.index + 1} / ${games.size}", color = Color.White.copy(.7f), fontSize = 11.sp)
                }
            }
        }

        // ---- title + hints -----------------------------------------------------------------
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            vm.selected?.let { g ->
                Text(g.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                val gb = g.size / 1_073_741_824.0
                val size = if (gb >= 1) "%.1f GB".format(gb) else "%d MB".format(g.size / 1_048_576)
                Text("${vm.consoleOf(g).name} · $size${if (g.favorite) " · ★ Favourite" else ""}",
                    color = Color.White.copy(.55f), fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            HintBar(listOf("L1 R1" to "System", "◀ ▶" to "Browse", "A" to "Play", "X" to "Details", "Y" to "Inspect", "☰" to "Settings", "SELECT" to "Steam mode"))
        }
    }
}

@Composable
fun HintBar(hints: List<Pair<String, String>>) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(.45f)).padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        hints.forEach { (k, v) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(k, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.White.copy(.18f)).padding(horizontal = 5.dp, vertical = 1.dp))
                Text(v, color = Color.White.copy(.7f), fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun animateColorAsState2(target: Color): State<Color> =
    androidx.compose.animation.animateColorAsState(target, androidx.compose.animation.core.tween(450), label = "bg")
