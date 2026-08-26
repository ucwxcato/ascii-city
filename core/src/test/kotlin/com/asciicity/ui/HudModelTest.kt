package com.asciicity.ui

import com.asciicity.render.ViewPose
import com.asciicity.system.SystemTestWorld
import com.asciicity.world.GridCell
import com.asciicity.world.WorldPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HudModelTest {
    @Test
    fun `compass reports eight headings with wrapped degrees`() {
        assertEquals("N 000°", HudText.compass(0.0))
        assertEquals("NE 045°", HudText.compass(Math.toRadians(45.0)))
        assertEquals("E 090°", HudText.compass(Math.toRadians(90.0)))
        assertEquals("S 180°", HudText.compass(Math.toRadians(180.0)))
        assertEquals("NW 315°", HudText.compass(Math.toRadians(-45.0)))
    }

    @Test
    fun `location and debug text expose immutable display values`() {
        val pose = ViewPose(
            position = WorldPosition(12.25, 7.5),
            eyeHeight = 0.6,
            yawRadians = Math.toRadians(90.0),
            pitchRadians = Math.toRadians(-10.0),
            horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
        )

        assertEquals("X 012.25  Z 007.50  |  E 090°", HudText.location(pose))
        val debug = HudText.debug(
            mapId = "city-alpha",
            nextMapId = "water-park",
            fps = 60,
            columns = 128,
            rows = 40,
            glyphScale = 1,
            profileId = "standard",
            pose = pose,
            effectiveHorizontalFovRadians = pose.horizontalFovRadians,
            horizonRow = 25.5,
            rayCount = 128,
            ddaSteps = 640,
            filledGlyphCount = 2400,
            targetId = "terminal.archive",
            targetDepth = 1.25,
            cellId = "road",
            groundMaterialId = "ground.road",
            facadeMaterialId = "facade.none",
            crtEffectEnabled = false,
        )

        assertTrue("MAP city-alpha | [TAB] LOAD water-park" in debug)
        assertTrue("FPS 60 | GRID 128x40 @1x | PROFILE STANDARD" in debug)
        assertTrue("FOV 75.0" in debug)
        assertTrue("TARGET terminal.archive @ 1.250" in debug)
        assertTrue("CELL road" in debug)
        assertTrue("ground.road / facade.none" in debug)
        assertTrue("CRT OFF | [C] TOGGLE" in debug)
    }

    @Test
    fun `debug footprint is north-up bounded and marks player`() {
        val world = SystemTestWorld.create(width = 5, depth = 5)

        val footprint = DebugFootprint.render(world.grid, GridCell(0, 0), halfWidth = 1, halfDepth = 1)
        val rows = footprint.lines()

        assertEquals(3, rows.size)
        assertEquals(" ..", rows[0])
        assertEquals(" @.", rows[1])
        assertEquals("   ", rows[2])
        assertEquals(1, footprint.count { it == '@' })
    }

    @Test
    fun `empty objective and signal remain valid HUD state`() {
        val model = HudModel(locationLine = "X 000.00  Z 000.00  |  N 000°")

        assertTrue(model.objectiveLine.isEmpty())
        assertTrue(model.signalLine.isEmpty())
        assertFalse(model.debugVisible)
    }

    @Test
    fun `music control reports available muted and unavailable states`() {
        assertEquals("[M] MUSIC ON", HudText.music(available = true, muted = false))
        assertEquals("[M] MUSIC OFF", HudText.music(available = true, muted = true))
        assertEquals("[M] MUSIC N/A", HudText.music(available = false, muted = false))
        assertTrue("M Music" in HudText.CONTROLS)
    }
}
