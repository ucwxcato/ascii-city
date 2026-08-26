package com.asciicity.ui

import com.asciicity.render.ViewPose
import com.asciicity.world.GridCell
import com.asciicity.world.WorldGrid
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt

data class HudModel(
    val locationLine: String,
    val objectiveLine: String = "",
    val signalLine: String = "",
    val promptLine: String = "",
    val debugVisible: Boolean = false,
    val debugText: String = "",
    val debugMap: String = "",
    val musicAvailable: Boolean = false,
    val musicMuted: Boolean = false,
)

object HudText {
    const val CONTROLS = "WASD Walk | Arrows Look | E Interact | Esc Back | M Music"

    fun music(available: Boolean, muted: Boolean): String = when {
        !available -> "[M] MUSIC N/A"
        muted -> "[M] MUSIC OFF"
        else -> "[M] MUSIC ON"
    }

    fun location(pose: ViewPose): String = String.format(
        Locale.ROOT,
        "X %06.2f  Z %06.2f  |  %s",
        pose.position.x,
        pose.position.z,
        compass(pose.yawRadians),
    )

    fun compass(yawRadians: Double): String {
        require(yawRadians.isFinite()) { "HUD yaw must be finite" }
        val degrees = Math.toDegrees(ViewPose.normalizeYaw(yawRadians))
        val heading = COMPASS_POINTS[((degrees / 45.0).roundToInt()) % COMPASS_POINTS.size]
        val wholeDegrees = degrees.roundToInt() % 360
        return "%s %03d°".format(Locale.ROOT, heading, wholeDegrees)
    }

    fun prompt(label: String): String = "[E] ${label.trim().ifEmpty { "INTERACT" }}"

    fun debug(
        mapId: String,
        nextMapId: String?,
        fps: Int,
        columns: Int,
        rows: Int,
        glyphScale: Int,
        profileId: String,
        pose: ViewPose,
        effectiveHorizontalFovRadians: Double,
        horizonRow: Double,
        rayCount: Int,
        ddaSteps: Int,
        filledGlyphCount: Int,
        targetId: String?,
        targetDepth: Double?,
        cellId: String,
        groundMaterialId: String,
        facadeMaterialId: String,
        crtEffectEnabled: Boolean,
    ): String = buildString {
        append("MAP ").append(mapId)
        if (nextMapId != null) append(" | [TAB] LOAD ").append(nextMapId)
        append('\n')
        append("FPS ").append(fps)
            .append(" | GRID ").append(columns).append('x').append(rows)
            .append(" @").append(glyphScale).append('x')
            .append(" | PROFILE ").append(profileId.uppercase(Locale.ROOT))
            .append('\n')
        append(
            "POSE x=%.3f z=%.3f yaw=%.2f pitch=%.2f".format(
                Locale.ROOT,
                pose.position.x,
                pose.position.z,
                Math.toDegrees(pose.yawRadians),
                Math.toDegrees(pose.pitchRadians),
            ),
        ).append('\n')
        append(
            "FOV %.1f | HORIZON %.2f".format(
                Locale.ROOT,
                effectiveHorizontalFovRadians * 180.0 / PI,
                horizonRow,
            ),
        ).append('\n')
        append("RAYS ").append(rayCount)
            .append(" | STEPS ").append(ddaSteps)
            .append(" | GLYPHS ").append(filledGlyphCount)
            .append('\n')
        append("TARGET ").append(targetId ?: "--")
        if (targetDepth != null) append(" @ %.3f".format(Locale.ROOT, targetDepth))
        append('\n')
        append("CELL ").append(cellId).append('\n')
        append("MAT ").append(groundMaterialId).append(" / ").append(facadeMaterialId)
            .append('\n')
        append("CRT ").append(if (crtEffectEnabled) "ON" else "OFF").append(" | [C] TOGGLE")
    }

    private val COMPASS_POINTS = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
}

object DebugFootprint {
    fun render(
        grid: WorldGrid,
        playerCell: GridCell,
        halfWidth: Int = 6,
        halfDepth: Int = 4,
    ): String {
        require(halfWidth >= 0 && halfDepth >= 0) { "Debug footprint radii must be non-negative" }
        return buildString {
            for (z in playerCell.z + halfDepth downTo playerCell.z - halfDepth) {
                for (x in playerCell.x - halfWidth..playerCell.x + halfWidth) {
                    when {
                        x == playerCell.x && z == playerCell.z -> append('@')
                        grid.contains(x, z) -> append(grid.cellAt(x, z).token)
                        else -> append(' ')
                    }
                }
                if (z > playerCell.z - halfDepth) append('\n')
            }
        }
    }
}
