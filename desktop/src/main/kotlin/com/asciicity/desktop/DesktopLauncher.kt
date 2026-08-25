package com.asciicity.desktop

import com.asciicity.AsciiCityGame
import com.asciicity.asset.AssetPaths
import com.asciicity.content.MapCatalog
import com.asciicity.render.AsciiFontProfile
import com.asciicity.world.WorldPosition
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.glutils.HdpiMode

private const val SMOKE_TEST_FRAMES = 24
private const val WALK_VERIFICATION_FRAMES = 750
private const val INTERACTION_VERIFICATION_FRAMES = 210
private const val CASINO_ENTRY_VERIFICATION_FRAMES = 40
private const val PRESENTATION_VERIFICATION_FRAMES = 540

fun main(arguments: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler(FatalErrorHandler())

    val requestedWindowSize = RequestedWindowSize.from(arguments)
        ?: RequestedWindowSize(
            width = DesktopWindowSpec.DEFAULT_WIDTH,
            height = DesktopWindowSpec.DEFAULT_HEIGHT,
        )
    val configuration = Lwjgl3ApplicationConfiguration()
    configuration.setTitle(DesktopWindowSpec.TITLE)
    configuration.setWindowedMode(requestedWindowSize.width, requestedWindowSize.height)
    configuration.setWindowSizeLimits(
        DesktopWindowSpec.MINIMUM_WIDTH,
        DesktopWindowSpec.MINIMUM_HEIGHT,
        DesktopWindowSpec.UNBOUNDED_SIZE,
        DesktopWindowSpec.UNBOUNDED_SIZE,
    )
    configuration.setResizable(true)
    configuration.useVsync(true)
    configuration.setForegroundFPS(DesktopWindowSpec.FOREGROUND_FPS)
    configuration.setHdpiMode(HdpiMode.Logical)

    val verifyWalk = "--verify-walk" in arguments
    val verifyInteraction = "--verify-interaction" in arguments
    val verifyCasinoEntry = "--verify-casino-entry" in arguments
    val verifyCasinoReturn = "--verify-casino-return" in arguments
    val verifyPresentation = "--verify-presentation" in arguments
    require(listOf(verifyWalk, verifyInteraction, verifyCasinoEntry, verifyCasinoReturn, verifyPresentation).count { it } <= 1) {
        "Choose only one verification route"
    }
    val exitAfterFrames = when {
        verifyWalk -> WALK_VERIFICATION_FRAMES
        verifyInteraction -> INTERACTION_VERIFICATION_FRAMES
        verifyCasinoEntry -> CASINO_ENTRY_VERIFICATION_FRAMES
        verifyCasinoReturn -> CASINO_ENTRY_VERIFICATION_FRAMES
        verifyPresentation -> PRESENTATION_VERIFICATION_FRAMES
        "--smoke-test" in arguments -> SMOKE_TEST_FRAMES
        else -> null
    }
    val screenshotPath = argumentValue(arguments, "--screenshot=")
    val worldFontProfile = argumentValue(arguments, "--ascii-density=")
        ?.let(AsciiFontProfile::fromId)
        ?: AsciiFontProfile.DEFAULT
    val initialPosition = RequestedViewPosition.from(arguments)?.let { WorldPosition(it.x, it.z) }
    val assetPaths = AssetPaths(
        font = argumentValue(arguments, "--font-path=") ?: AssetPaths.DEFAULT_FONT,
        palette = argumentValue(arguments, "--palette-path=") ?: AssetPaths.DEFAULT_PALETTE,
    )
    val initialMapId = if (verifyCasinoReturn) "casino" else argumentValue(arguments, "--map=") ?: MapCatalog.DEFAULT_MAP_ID
    require(initialMapId in MapCatalog.playableMapIds) {
        "--map must name a playable map: ${MapCatalog.playableMapIds.joinToString()}"
    }
    Lwjgl3Application(
        AsciiCityGame(
            exitAfterFrames = exitAfterFrames,
            screenshotPath = screenshotPath,
            assetPaths = assetPaths,
            initialYawDegrees = finiteArgument(arguments, "--view-yaw="),
            initialPitchDegrees = finiteArgument(arguments, "--view-pitch="),
            initialPosition = initialPosition,
            verifyWalk = verifyWalk,
            verifyInteraction = verifyInteraction,
            verifyCasinoEntry = verifyCasinoEntry,
            verifyCasinoReturn = verifyCasinoReturn,
            verifyPresentation = verifyPresentation,
            audioEnabled = "--no-audio" !in arguments,
            headBobEnabled = "--no-head-bob" !in arguments,
            initialMapId = initialMapId,
            worldFontProfile = worldFontProfile,
            screenshotFrame = when {
                verifyWalk -> 600
                verifyInteraction -> 20
                verifyCasinoEntry -> 20
                verifyCasinoReturn -> 20
                verifyPresentation -> 60
                "--smoke-test" in arguments -> 22
                else -> 4
            },
        ),
        configuration,
    )
}

private fun argumentValue(arguments: Array<String>, prefix: String): String? = arguments
    .firstOrNull { it.startsWith(prefix) }
    ?.removePrefix(prefix)

private fun finiteArgument(arguments: Array<String>, prefix: String): Double? {
    val raw = argumentValue(arguments, prefix) ?: return null
    val value = raw.toDoubleOrNull()
    require(value != null && value.isFinite()) { "$prefix requires a finite number of degrees" }
    return value
}

data class RequestedViewPosition(
    val x: Double,
    val z: Double,
) {
    companion object {
        fun from(arguments: Array<String>): RequestedViewPosition? {
            val x = finiteArgument(arguments, "--view-x=")
            val z = finiteArgument(arguments, "--view-z=")
            require((x == null) == (z == null)) { "--view-x and --view-z must be provided together" }
            return if (x == null) null else RequestedViewPosition(x, checkNotNull(z))
        }
    }
}
