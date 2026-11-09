package com.shelfdeck.ui

import android.widget.Toast
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import com.shelfdeck.MainViewModel
import com.shelfdeck.Overlay
import com.shelfdeck.UiMode

@Composable
fun AppRoot(vm: MainViewModel) {
    val focus = remember { FocusRequester() }
    val ctx = LocalContext.current
    val pickArt = rememberArtPicker(vm)

    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(vm.toast) {
        vm.toast?.let { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show(); vm.toast = null }
    }

    Box(
        Modifier.fillMaxSize().focusRequester(focus).focusable().onPreviewKeyEvent { e ->
            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val ov = vm.overlay
            when (e.key) {
                Key.ButtonB, Key.Escape, Key.Back -> {
                    if (ov == Overlay.BOOT) vm.cancelBoot() else if (ov != Overlay.NONE) vm.closeOverlay() else return@onPreviewKeyEvent false
                    true
                }
                Key.DirectionLeft, Key.DirectionRight -> {
                    val d = if (e.key == Key.DirectionLeft) -1 else 1
                    when (ov) {
                        Overlay.NONE -> if (vm.mode == UiMode.SHELF) { vm.move(d); true } else false
                        Overlay.VIEWER -> { vm.viewerPage = (vm.viewerPage + d + 4) % 4; true }
                        else -> false
                    }
                }
                Key.ButtonL1 -> { if (ov == Overlay.NONE) { vm.cycleConsole(-1); true } else false }
                Key.ButtonR1 -> { if (ov == Overlay.NONE) { vm.cycleConsole(1); true } else false }
                Key.ButtonA, Key.Enter, Key.NumPadEnter -> when (ov) {
                    Overlay.NONE -> { (if (vm.mode == UiMode.SHELF) vm.selected else vm.games.firstOrNull { it.id == vm.steamSelected })?.let { vm.play(it) }; true }
                    Overlay.VIEWER -> { vm.viewerPage = (vm.viewerPage + 1) % 4; true }
                    else -> false
                }
                Key.ButtonX -> when (ov) {
                    Overlay.NONE -> { vm.overlay = Overlay.DETAILS; true }
                    Overlay.VIEWER -> { vm.selected?.let { pickArt(it, com.shelfdeck.ArtKinds[vm.viewerPage]) }; true }
                    else -> false
                }
                Key.ButtonY -> { if (ov == Overlay.NONE) { vm.openViewer(); true } else false }
                Key.ButtonStart -> { if (ov == Overlay.NONE) { vm.overlay = Overlay.SETTINGS; true } else false }
                Key.ButtonSelect -> { if (ov == Overlay.NONE) { vm.toggleMode(); true } else false }
                else -> false
            }
        },
    ) {
        when (vm.mode) {
            UiMode.SHELF -> ShelfScreen(vm)
            UiMode.STEAM -> SteamScreen(vm)
        }
        when (vm.overlay) {
            Overlay.DETAILS -> DetailsPanel(vm)
            Overlay.VIEWER -> ViewerOverlay(vm)
            Overlay.SETTINGS -> SettingsScreen(vm)
            Overlay.BOOT -> BootScreen(vm)
            Overlay.NONE -> {}
        }
    }
}
