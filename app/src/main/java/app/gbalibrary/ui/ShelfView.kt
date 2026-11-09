package app.gbalibrary.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.gbalibrary.data.ConsoleDef
import app.gbalibrary.data.Game
import app.gbalibrary.data.Systems
import kotlin.math.abs

/** Consola de un juego (integrada o externa) a partir de la etiqueta que da [systemOf]. */
fun consoleOf(g: Game): ConsoleDef = Systems.byShort(systemOf(g)) ?: Systems.builtIn[0]

/**
 * Estanteria 3D de cajas (como el frontend del video): la caja elegida al centro, las demas giradas
 * a los lados con reflejo. Desliza o usa el D-pad / stick; toca la caja central para ver la ficha.
 * El indice elegido lo lleva quien la llama ([selected]) para compartir el foco del mando.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShelfView(
    games: List<Game>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onOpen: (Game) -> Unit,
    onPlay: (Game) -> Unit,
    onLong: (Game) -> Unit,
) {
    val p = LocalPalette.current
    val idx = selected.coerceIn(0, (games.size - 1).coerceAtLeast(0))
    val cur = games.getOrNull(idx)
    val console = if (cur != null) consoleOf(cur) else Systems.builtIn[0]
    val pos by animateFloatAsState(idx.toFloat(), spring(stiffness = Spring.StiffnessMediumLow), label = "shelfPos")
    val top by animateColorAsState(console.bgTop, tween(450), label = "shelfTop")
    val density = LocalDensity.current.density
    val idxNow by rememberUpdatedState(idx)
    val sizeNow by rememberUpdatedState(games.size)

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, console.bgBottom)))) {
        if (cur == null) {
            Text("No hay juegos con este filtro", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        BoxWithConstraints(
            Modifier.fillMaxSize().padding(bottom = 96.dp).pointerInput(Unit) {
                var acc = 0f
                detectHorizontalDragGestures(onDragStart = { acc = 0f }) { _, dx ->
                    acc += dx
                    if (acc > 100f) { onSelect((idxNow - 1).coerceAtLeast(0)); acc = 0f }
                    else if (acc < -100f) { onSelect((idxNow + 1).coerceAtMost(sizeNow - 1)); acc = 0f }
                }
            },
        ) {
            val cardH = maxHeight * 0.66f
            val cardW = cardH * console.boxRatio
            val cardWpx = with(LocalDensity.current) { cardW.toPx() }

            for (i in (idx - 7)..(idx + 7)) {
                val g = games.getOrNull(i) ?: continue
                key(g.id) {
                    val d = i - pos
                    val near = d.coerceIn(-1f, 1f)
                    val far = d - near
                    val scale = if (abs(d) < 1f) 1.18f - 0.28f * abs(d) else 0.90f
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .offset(y = -cardH * 0.03f)
                            .size(cardW, cardH)
                            .zIndex(-abs(d))
                            .graphicsLayer {
                                translationX = near * cardWpx * 1.25f + far * cardWpx * 0.36f
                                rotationY = -near * 58f
                                scaleX = scale
                                scaleY = scale
                                cameraDistance = 14f * density
                                alpha = (1f - (abs(d) - 4f).coerceAtLeast(0f) * 0.3f).coerceIn(0f, 1f)
                            }
                            .combinedClickable(
                                onClick = { if (i == idxNow) onOpen(g) else onSelect(i) },
                                onLongClick = { onLong(g) },
                            ),
                    ) {
                        CoverImage(g, Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)))
                        // reflejo sobre la "repisa"
                        Box(
                            Modifier
                                .offset(y = cardH)
                                .size(cardW, cardH)
                                .graphicsLayer {
                                    scaleY = -1f
                                    alpha = 0.28f
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black),
                                        blendMode = BlendMode.DstIn,
                                    )
                                },
                        ) { CoverImage(g, Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) }
                    }
                }
            }
        }

        // ---- titulo + acciones ----
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                cur.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                "${console.name} · ${formatPlayTime(cur.playSeconds)} · ${idx + 1} / ${games.size}",
                color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "JUGAR", color = p.onPlay, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(p.play)
                        .clickable { onPlay(cur) }.padding(horizontal = 26.dp, vertical = 8.dp),
                )
                Text(
                    "Detalles", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.14f))
                        .clickable { onOpen(cur) }.padding(horizontal = 22.dp, vertical = 8.dp),
                )
            }
        }
    }
}
