package app.gbalibrary.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Ic(icon: ImageVector, size: Dp = 24.dp, tint: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    Icon(imageVector = icon, contentDescription = null, modifier = modifier.size(size), tint = tint)
}
