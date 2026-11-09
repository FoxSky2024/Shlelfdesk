package com.shelfdeck.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Steam {
    val header = Color(0xFF171A21)
    val bg = Color(0xFF1B2838)
    val panel = Color(0xFF16202D)
    val blue = Color(0xFF66C0F4)
    val text = Color(0xFFC7D5E0)
    val greenA = Color(0xFF75B022)
    val greenB = Color(0xFF588A1B)
}

@Composable
fun ShelfDeckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Color(0xFF8E7CFF), background = Color.Black, surface = Color(0xFF12111C)),
        content = content,
    )
}
