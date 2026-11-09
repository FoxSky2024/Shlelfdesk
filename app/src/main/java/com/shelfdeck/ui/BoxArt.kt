package com.shelfdeck.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shelfdeck.MainViewModel
import com.shelfdeck.data.Game

/**
 * Box art for one face (front/back/disc/manual). Uses an imported image when present,
 * otherwise draws a generated cover so the shelf is never empty.
 */
@Composable
fun BoxArt(vm: MainViewModel, game: Game, kind: String = "front", modifier: Modifier = Modifier) {
    val console = vm.consoleOf(game)
    val file = remember(game.id, kind, vm.artVersion) { vm.store.artFile(game, kind).takeIf { it.exists() } }
    Box(modifier.clip(RoundedCornerShape(4.dp))) {
        if (file != null) {
            AsyncImage(file, game.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Column(
                Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(console.accent, console.bgTop, Color.Black))),
            ) {
                Box(Modifier.fillMaxWidth().background(Color.Black.copy(.45f)).padding(vertical = 6.dp), Alignment.Center) {
                    Text(console.name.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                Box(Modifier.weight(1f).padding(10.dp), Alignment.Center) {
                    Text(
                        if (kind == "front") game.title else "${kind.uppercase()}\n${game.title}",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center, maxLines = 5,
                    )
                }
            }
        }
        // glossy edge highlight so boxes read as objects
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color.White.copy(.18f), Color.Transparent, Color.Black.copy(.25f)))))
    }
}
