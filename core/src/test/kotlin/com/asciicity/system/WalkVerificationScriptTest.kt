package com.asciicity.system

import com.asciicity.content.ContentLoader
import com.asciicity.input.InputController
import com.asciicity.input.WalkVerificationScript
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WalkVerificationScriptTest {
    @Test
    fun `authored route traverses the expanded park collides and returns to spawn`() {
        val content = ContentLoader().load(Path.of("."), "city-alpha")
        val spawn = content.world.player.position
        val input = InputController()
        val simulation = PlayerSimulation(content.world, input)
        val script = WalkVerificationScript()
        val events = mutableListOf<WalkVerificationScript.Event>()
        var maximumX = spawn.x

        repeat(686) { step ->
            script.apply(step, input)?.let(events::add)
            simulation.step(1.0 / 60.0)
            maximumX = maxOf(maximumX, simulation.currentState.position.x)
        }

        assertEquals(WalkVerificationScript.Event.entries.toList(), events)
        assertEquals(null, script.apply(685, input), "a repeated resize/render step must not fire twice")
        assertTrue(maximumX in 32.70..32.80, "route should enter the park and contact the litter-bin cell, max x=$maximumX")
        assertEquals(spawn.x, simulation.currentState.position.x, 0.01)
        assertEquals(spawn.z, simulation.currentState.position.z, 0.01)
        assertEquals(0.0, simulation.currentState.yawRadians, 1e-10)
        assertEquals(0.0, simulation.currentState.pitchRadians, 1e-10)
    }
}
