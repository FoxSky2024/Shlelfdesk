package com.shelfdeck.engine

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.shelfdeck.data.ConsoleDef
import com.shelfdeck.data.EmulatorDef
import com.shelfdeck.data.Game

sealed interface LaunchResult {
    data class Ok(val emulator: EmulatorDef) : LaunchResult
    data class NoEmulatorConfigured(val console: ConsoleDef) : LaunchResult
    data class NotInstalled(val console: ConsoleDef) : LaunchResult
    data class Failed(val reason: String) : LaunchResult
}

/**
 * The "engine": per-system emulator profiles, installed-emulator detection and game launching.
 *
 * It does not interpret console CPUs itself - it routes each game to the real emulator app
 * registered for that system (see [com.shelfdeck.data.Systems]) and hands it a read-granted content URI.
 */
class EmulationEngine(private val ctx: Context) {

    fun isInstalled(e: EmulatorDef): Boolean = runCatching {
        ctx.packageManager.getPackageInfo(e.pkg, 0); true
    }.getOrDefault(false)

    /** First installed emulator for the console, or null. */
    fun resolve(console: ConsoleDef): EmulatorDef? = console.emulators.firstOrNull(::isInstalled)

    fun launch(activity: Activity, game: Game, console: ConsoleDef): LaunchResult {
        if (console.emulators.isEmpty()) return LaunchResult.NoEmulatorConfigured(console)
        val emu = resolve(console) ?: return LaunchResult.NotInstalled(console)

        fun build(withComponent: Boolean) = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(game.uri), "application/octet-stream")
            if (withComponent && emu.activity != null) component = ComponentName(emu.pkg, emu.activity)
            else setPackage(emu.pkg)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

        return try {
            activity.startActivity(build(withComponent = true)); LaunchResult.Ok(emu)
        } catch (_: Exception) {
            // Explicit component may not exist in this version/fork: let the OS resolve inside the package.
            try {
                activity.startActivity(build(withComponent = false)); LaunchResult.Ok(emu)
            } catch (e: Exception) {
                LaunchResult.Failed(e.message ?: "Could not start ${emu.label}")
            }
        }
    }

    fun openStorePage(activity: Activity, e: EmulatorDef) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${e.pkg}"))
        try { activity.startActivity(market) } catch (_: Exception) {
            activity.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=${e.pkg}")))
        }
    }
}
